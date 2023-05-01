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
import com.xiaomi.duckling.dimension.matcher.Prods.{regexMatch, singleRegexMatch}
import com.xiaomi.duckling.dimension.time.{form, Time, TimeData}
import com.xiaomi.duckling.dimension.time.duration.{Duration, DurationData}
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.predicates.{isAPartOfDay, isATimeOfDay, isNotLatent, isTimeDatePredicate, TimeDatePredicate}

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
    pattern = List("每(一|一个|个)?".regex, isTimeDatePredicate.predicate),
    prod = tokens {
      case _ :: (t @ Token(_, td: TimeData)) :: _ =>
        val grainOfInterval = td.timePred.asInstanceOf[TimeDatePredicate].maxGrain.get
        val grain =
          if (grainOfInterval == Grain.Hour && (isAPartOfDay(t) || isATimeOfDay(t))) Grain.Day
          else grainOfInterval
        val interval = DurationData(1, grain)
        Token(Repeat, RepeatData(interval, start = td))
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

  val ruleEveryGrainDatetime = Rule(
    name = "<every> <grain> <datetime>",
    pattern = List(
      "每(一个?|个)?(年度?|月|周|星期|天|小时|分钟)的?".regex,
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
    pattern = List("(每一?个)?(工作日|非工作日|节假日)".regex),
    prod = regexMatch { case _ :: _ :: t :: _ =>
    val workdayType = t match {
        case "工作日" => WorkdayType.Workday
        case "非工作日" | "节假日" => WorkdayType.NonWorkday
      }
      Token(Repeat, RepeatData(workdayType = Some(workdayType)))
    }
  )

  val ruleWorkDaysTime = Rule(
    name = "<work/non-workday> <time>",
    pattern = List(isOnlyWorkdaysType.predicate, isHourTimes.predicate),
    prod = tokens { case Token(Repeat, rd: RepeatData) :: Token(Time, td: TimeData) :: _ =>
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
  )
}
