/*
 * Copyright (c) 2020, Xiaomi and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.duckling.dimension.time.repeat

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.matcher.Prods.regexMatch
import com.xiaomi.duckling.dimension.time.{form, GrainWrapper, Time, TimeData}
import com.xiaomi.duckling.dimension.time.duration.{Duration, DurationData}
import com.xiaomi.duckling.dimension.time.enums.{Grain, Hint}
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers.intersect
import com.xiaomi.duckling.dimension.time.predicates._

trait Rules extends DimRules with LazyLogging {
  /**
   * 每隔? Duration..
   */
  val ruleEveryDuration = Rule(
    name = "<every> <duration>",
    pattern = List("(每隔|每|隔)".regex, isDimension(Duration).predicate),
    prod = tokens {
      case _ :: Token(Duration, interval: DurationData) :: _ =>
        Token(Repeat, RepeatData(interval))
    }
  )

  /**
   * 每周三 / 每[个]月[的]三号 / 每年三月 / 每年三月一号 / 每[一]天[的][下午]三点
   */
  val ruleEveryDatetime = Rule(
    name = "<every> <datetime>",
    pattern = List("每(一个|一|个)?".regex, or(isTimeDatePredicate, isHint(Hint.PartOfDayAtLast), isAPartOfDay).predicate),
    prod = tokens {
      case _ :: (t@Token(_, td: TimeData)) :: _ if td.timePred.maxGrain.nonEmpty =>
        val maxGrain = td.timePred.maxGrain.get
        val grain =
          if (maxGrain == Grain.Hour && (isAPartOfDay(t) || isATimeOfDay(t))) Grain.Day
          else maxGrain
        val interval = DurationData(1, grain)
        td.timePred match {
          case _: TimeDatePredicate | _: TimeIntervalsPredicate | _: IntersectTimePredicate =>
            Token(Repeat, RepeatData(interval, start = td))
          case _ => None
        }
    }
  )

  private def toGrain(grain: String): Option[Grain] = {
    grain match {
      case "年" | "年度" => Grain.Year
      case "月" => Grain.Month
      case "周" | "星期" => Grain.Week
      case "日" | "天" => Grain.Day
      case "小时" => Grain.Hour
      case "分钟" => Grain.Minute
      case _ => None
    }
  }

  val ruleEveryGrain = Rule(
    name = "<every> <grain>",
    pattern = List("每(一个?|个)?(年度?|月|周|星期|天|小时|分钟)".regex),
    prod = tokens {
      case Token(_, GroupMatch(_ :: _ :: grain :: _)) :: _ =>
        val grainHint: Option[Grain] = toGrain(grain)
        if (grainHint.nonEmpty) {
          val interval = DurationData(1, grainHint.get)
          Token(Repeat, RepeatData(interval))
        } else {
          logger.warn(s"unmatched grain found: ${grain}, please feedback to fix it")
          None
        }
    }
  )

  private val predicateEveryGrain = "每(一个?|个)?(年度?|月|周|星期|天|小时|分钟)的?".regex

  val ruleEveryGrainDatetime = Rule(
    name = "<every> <grain> <datetime>",
    pattern = List(
      predicateEveryGrain,
      and(isDimension(Time), isNotLatent).predicate),
    prod = tokens {
      case Token(_, GroupMatch(_ :: _ :: grainToken :: _)) :: Token(_, td: TimeData) :: _
        if td.timePred.maxGrain.nonEmpty =>
        val grainOfInterval = td.timePred.maxGrain.get
        val grainHint: Option[Grain] = toGrain(grainToken)
        if (grainHint.nonEmpty && grainHint.get.finer().contains(grainOfInterval)) {
          val interval = DurationData(1, grainHint.getOrElse(grainOfInterval))
          (Token(Repeat, RepeatData(interval, start = td)))
        } else {
          logger.warn(s"unmatched grain found: ${grainToken}, please feedback to fix it")
          None
        }
    }
  )

  val ruleWorkDays = Rule(
    name = "<work/non-workday>",
    pattern = List("(每一?个)?(工作日|非工作日|节假日)(每天)?".regex),
    prod = regexMatch { case _ :: _ :: t :: _ =>
    val workdayType = t match {
        case "工作日" => WorkdayType.Workday
        case "非工作日" | "节假日" => WorkdayType.NonWorkday
      }
      Token(Repeat, RepeatData(workdayType = Some(workdayType)))
    }
  )

  def workdaysTime(rd: RepeatData, td: TimeData) = {
    // 工作日八点应该就是上午八点，不是晚上八点
    val _form = td.form match {
      case Some(form.TimeOfDay(h, true)) => Some(form.TimeOfDay(h, false))
      case _ => td.form
    }
    val timePred = td.timePred match {
      case tdp: TimeDatePredicate =>
        val _hour = tdp.hour match {
          case Some((true, h)) => Some((false, h))
          case _ => tdp.hour
        }
        tdp.copy(hour = _hour)
      case _ => td.timePred
    }
    Token(Repeat, RepeatData(workdayType = rd.workdayType, start = td.copy(timePred = timePred, form = _form)))
  }

  val ruleWorkDaysTime1 = Rule(
    name = "<work/non-workday> <time>",
    pattern = List(isOnlyWorkdaysType.predicate, isHourTimes.predicate),
    prod = tokens { case Token(Repeat, rd: RepeatData) :: Token(Time, td: TimeData) :: _ =>
      workdaysTime(rd, td)
    }
  )

  val ruleWorkDaysTime2 = Rule(
    name = "<work/non-workday> 的 <time>",
    pattern = List(isOnlyWorkdaysType.predicate, "的".regex, isHourTimes.predicate),
    prod = tokens { case Token(Repeat, rd: RepeatData) :: _ :: Token(Time, td: TimeData) :: _ =>
      workdaysTime(rd, td)
    }
  )

  // 周一到周五早上八点
  val ruleIntervalTime = Rule(
    name = "<interval> <time/interval>",
    pattern = List(isInterval.predicate, isDimension(Time).predicate),
    prod = tokens { case Token(Time, outer: TimeData) :: Token(Time, inner: TimeData):: _
      if outer.timeGrain > inner.timeGrain && (inner.timePred.maxGrain.isEmpty || outer.timeGrain > inner.timePred.maxGrain.get) =>
      val oInterval = outer.timePred.asInstanceOf[TimeIntervalsPredicate]
      // start
      val start = intersect(inner, TimeData(oInterval.p1, timeGrain=outer.timeGrain))
      Token(Repeat, RepeatData(start = start, repeatNFromInterval = outer))
    }
  )

  // 周一到周五早上的八点
  val ruleIntervalTime1 = Rule(
    name = "<interval> 的 <time/interval>",
    pattern = List(isInterval.predicate, "的".regex, isDimension(Time).predicate),
    prod = tokens { case Token(Time, outer: TimeData) :: _ :: Token(Time, inner: TimeData):: _ if outer.timeGrain > inner.timeGrain =>
      val oInterval = outer.timePred.asInstanceOf[TimeIntervalsPredicate]
      val start = intersect(inner.copy(hint = Hint.NoHint), TimeData(oInterval.p1, timeGrain=outer.timeGrain).copy(hint = Hint.NoHint))
      Token(Repeat, RepeatData(start = start, repeatNFromInterval = outer))
    }
  )

  val ruleEveryRepeat = Rule(
    name = "每 x <repeat>",
    pattern = List(predicateEveryGrain, isDimension(Repeat).predicate),
    prod = tokens { case Token(_, GroupMatch(_ :: _ :: grainToken :: _)) :: Token(_, repeat: RepeatData):: _ =>
      val grainHint: Option[Grain] = toGrain(grainToken)
      (grainHint, repeat.repeatNFromInterval) match {
        case (Some(everyGrain), Some(td)) if everyGrain >= td.timeGrain =>
          val interval = DurationData(1, grainHint.getOrElse(everyGrain))
          Token(Repeat, repeat.copy(interval = interval))
        case _ => None
      }
    }
  )

  val ruleEveryRepeat1 = Rule(
    name = "每 <repeat>",
    pattern = List("每个?".regex, isDimension(Repeat).predicate),
    prod = tokens { case _ :: Token(_, repeat: RepeatData):: _ =>
      repeat.repeatNFromInterval match {
        case Some(td) if td.timePred.maxGrain.isDefined =>
          val interval = DurationData(1, td.timePred.maxGrain.get)
          Token(Repeat, repeat.copy(interval = interval))
        case _ => None
      }
    }
  )
}
