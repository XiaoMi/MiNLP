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

package com.xiaomi.duckling.dimension.time

import scala.beans.BooleanBeanProperty

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.numeral.Predicates.isNatural
import com.xiaomi.duckling.dimension.numeral.seq.DigitSequence
import com.xiaomi.duckling.dimension.ordinal.Ordinal
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time.date.Date
import com.xiaomi.duckling.dimension.time.duration.Duration
import com.xiaomi.duckling.dimension.time.enums._
import com.xiaomi.duckling.dimension.time.form.{Form, PartOfDay, TimeOfDay}
import com.xiaomi.duckling.dimension.time.grain.TimeGrain
import com.xiaomi.duckling.dimension.time.helper.TimeObjectHelpers._
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.unitNumber.UnitNumber
import com.xiaomi.duckling.exceptions.LunarOutOfRangeException
import com.xiaomi.duckling.ranking.Types.{DiscreteFeature, Feature}
import com.xiaomi.duckling.types.Node

case object Time extends Dimension with Rules {
  override val name: String = "Time"

  override val dimDependents: List[Dimension] =
    List(Numeral, Ordinal, TimeGrain, UnitNumber, Duration, Date)

  override val nonOverlapDims: List[Dimension] = List(Numeral, DigitSequence, Ordinal)

  override def overlap(token: Token): Boolean = {
    token.dim match {
      case Numeral                 => isNatural(token)
      case DigitSequence | Ordinal => true
      case _                       => false
    }
  }

  override def combinationFeatures(node: Node): List[Feature] = {
    val s =
      if (node.rule.contains("<date>")) node.children(0).rule.get
      else node.rule.get
    List(DiscreteFeature(s))
  }
}

/**
 * 时间解析的额外参数，适应一些语义模糊的情况
 */
class TimeOptions {
  /**
   * 上午是否总是需要指今天的上午，默认是未来一个上午
   */
  @BooleanBeanProperty var resetTimeOfDay: Boolean = false
  /**
   * 最近是向前计算还是向后计算，默认是未来
   */
  @BooleanBeanProperty var recentInFuture: Boolean = true
  /**
   * 对于过去的日期是否取未来一年的日期
   */
  @BooleanBeanProperty var alwaysInFuture: Boolean = true
  /**
   * 继承来自持续时间的粒度，比如"三天后"，返回天级
   */
  @BooleanBeanProperty var inheritGrainOfDuration: Boolean = false
  /**
   * 解析春夏秋冬四季/输出结果以节气为参照
   */
  @BooleanBeanProperty var parseFourSeasons: Boolean = false
  /**
   * 是否支持明天的明天类的解析
   */
  @BooleanBeanProperty var sequence: Boolean = true
  /**
   * duration: 是否支持几个月/几天
   */
  @BooleanBeanProperty var durationFuzzyOn: Boolean = true
  /**
   * duration: 模糊的数量
   */
  @BooleanBeanProperty var durationFuzzyValue: Int = 3
}

case class TimeData(timePred: TimePredicate,
                    latent: Boolean = false,
                    timeGrain: Grain, // needed for intersect
                    notImmediate: Boolean = false,
                    form: Option[Form] = None,
                    direction: Option[IntervalDirection] = None,
                    okForThisNext: Boolean = false, // allows specific this+Time
                    holiday: Option[String] = None,
                    reset: Option[(Grain, Int)] = None, // 本周一，这个月三号，之类需要重置参考时间
                    calendar: Option[Calendar] = None, // 阳历农历标识
                    hint: Hint = Hint.NoHint, // 一些提示信息，比如不能够再组合，已经进行过时间组合（去年的今天）之类的
                    schema: Option[String] = None // 时间表达的归一值
) extends Resolvable
    with LazyLogging {
  override def resolve(context: Context, options: Options): Option[(TimeValue, Boolean)] = {

    val refTime =
      refTimeAdjust(new TimeObject(context.referenceTime, Grain.Second), options.timeOptions)

    val reverseTake = hint == Hint.UncertainRecent && !options.timeOptions.recentInFuture || !options.timeOptions.alwaysInFuture
    val valueOpt =
      try {
        resolveTimeData(refTime, this, reverseTake)
      } catch {
        case e: java.time.DateTimeException =>
          logger.error(s"time resolve failed with DateTimeException [${e.getMessage}]")
          None
        // 目前使用的农历包没有定义专用异常，可以到上游进行更改
        case e: java.lang.IllegalArgumentException =>
          logger.error(s"time resolve failed with IllegalArgumentException [${e.getMessage}]")
          None
      }

    if (hint == Hint.ComposeNeeded) None
    else if (valueOpt.isEmpty) None
    else {
      val partOfDay = form match {
        case Some(PartOfDay(part)) => Some(part)
        case _                     => None
      }
      try {
        val tv =
          TimeValue(
            timeValue(valueOpt.get),
            simple = generateSimple(timePred, holiday).scheme(),
            holiday = holiday,
            partOfDay = partOfDay,
            schema = schema
          )
        Some(tv, latent)
      } catch {
        case e: LunarOutOfRangeException =>
          logger.warn(e.getMessage)
          None
      }

    }
  }

  case class Simple(year: Option[Int] = None,
                    month: Option[Int] = None,
                    day: Option[Int] = None,
                    hour: Option[(Boolean, Int)] = None,
                    minute: Option[Int] = None,
                    second: Option[Int] = None,
                    offset: Option[Boolean] = None) {
    val y: String = year.map(v => "%04d".format(v)).getOrElse("x")
    val m: String = month.map(v => "%02d".format(v)).getOrElse("x")
    val d: String = day.map(v => "%02d".format(v)).getOrElse("x")
    val h: String = hour.map(v => "%02d".format(v._2)).getOrElse("x")
    val min: String = minute.map(v => "%02d".format(v)).getOrElse("x")
    val s: String = second.map(v => "%02d".format(v)).getOrElse("x")

    def scheme(): Option[(String, Boolean)] = {
      val simple = timeGrain match {
        case Grain.Year => s"$y-x-xTx:x:x"
        case Grain.Month => s"$y-$m-xTx:x:x"
        case Grain.Day => s"$y-$m-${d}Tx:x:x"
        case Grain.Hour => s"$y-$m-${d}T$h:x:x"
        case Grain.Minute => s"$y-$m-${d}T$h:$min:x"
        case Grain.Second => s"$y-$m-${d}T$h:$min:$s"
        case _ => "x-x-xTx:x:x"
      }

      Some((simple, offset.getOrElse(true)))
    }
  }

  /**
    * 生成sentence中年月日时分秒的原始表达字段，并给出表达中是否存在根据当前时间运算的偏移
    * 当sentence中出现如：[今/明/后/前/上/下]+[天/年/月]等表达是，当做偏移offset=true
    * 对于节气、西方节日、除夕等需要根据具体年份查表才能知道具体日期的，simple字段不给出日期信息
    * @param timePred timePred
    * @param holiday  节日信息
    * @return
    */
  def generateSimple(timePred: TimePredicate, holiday: Option[String]): Simple = {
    timePred match {
      case TimeDatePredicate(second, minute, hour, _, _, dayOfMonth, month, year, _) =>
        // 如：2021年10月1日12点三十分
        Simple(year, month, dayOfMonth, hour, minute, second, offset=Some(false))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, _, _, _, _), IntersectTimePredicate(TimeDatePredicate(_, _, _, _, _, dayOfMonth, month, _, _), SeriesPredicate(_))) =>
        // 如： 明年三月一号十二点三十分, 年份隐式表达SeriesPredicate
        Simple(None, month, dayOfMonth, hour, minute, second, offset=Some(true))
      case IntersectTimePredicate(SequencePredicate(_), TimeDatePredicate(_, _, _, _, _, _, _, year, _)) =>
        // 如： 2021年除夕， 除夕需要根据农历最后一天计算具体日期，只抽取年，除夕SequencePredicate
        Simple(year, offset=Some(false))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, _, _, _, _), SequencePredicate(_)) =>
        // 如：除夕十二点三十分，没有年份表达，除夕SequencePredicate
        Simple(hour=hour, minute=minute, second=second, offset=Some(false))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, _, _, _, _), IntersectTimePredicate(SequencePredicate(_), SeriesPredicate(_))) =>
        // 如：今年除夕十二点三十分，年份隐式表达，除夕SequencePredicate
        Simple(hour=hour, minute=minute, second=second, offset=Some(true))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, _, _, _, _), IntersectTimePredicate(_, TimeDatePredicate(_, _, _, _, _, _, _, year, _))) =>
        // 如：2021年[除夕、节气、西方节日]十二点三十分，有具体年份表达
        Simple(year=year, hour=hour, minute=minute, second=second, offset=Some(false))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, _, _, _, _), IntersectTimePredicate(SeriesPredicate(_), SeriesPredicate(_))) =>
        // 如：明年[清明节、西方节日]十二点三十分，年份隐式表达，节气SeriesPredicate
        Simple(hour=hour, minute=minute, second=second, offset=Some(true))
      case IntersectTimePredicate(SeriesPredicate(_), TimeDatePredicate(second, minute, hour, _, _, _, _, year, _)) =>
        // 如：2021年[清明节、西方节日]，节气需要根据年份查表获取具体日期，只抽取年，节气SeriesPredicate
        Simple(year=year, hour=hour, minute=minute, second=second, offset=Some(false))
      case IntersectTimePredicate(TimeDatePredicate(second, minute, hour, _, _, dayOfMonth, month, _, _), SeriesPredicate(_)) =>
        // 如：[清明节、西方节日]十二点三十分，今年国庆节，明年中秋节，今天十二点三十分，年份/日期隐式表达和节气SeriesPredicate
        val offset = (second.isDefined || minute.isDefined || hour.isDefined) && holiday.exists(_.nonEmpty)
        Simple(None, month, dayOfMonth, hour, minute, second, offset=Some(!offset))
      case IntersectTimePredicate(SeriesPredicate(_), SeriesPredicate(_)) =>
        // 如：今年[清明节、西方节日]，年份隐式表达和节气SeriesPredicate
        Simple(offset=Some(true))
      case SeriesPredicate(_) =>
        // 如：[清明节、西方节日]西方节日和节气SeriesPredicate
        Simple(offset=if (holiday.exists(_.nonEmpty)) Some(false) else Some(true))
      case _ => Simple()
    }
  }

  def refTimeAdjust(ref: TimeObject, timeOptions: TimeOptions): TimeObject = {
    val isPartOfDay = form match {
      case Some(PartOfDay(_)) => true
      case Some(TimeOfDay(_, _)) => true
      case _ => false
    }
    val grainReset =
      if (timeOptions.resetTimeOfDay && isPartOfDay) timeRound(ref, Grain.Day)
      else {
        reset match {
          case Some((grain, n)) => timePlus(timeRound(ref, grain), grain, n)
          case None => ref
        }
      }
    val adjustByCal = grainReset.to(calendar)
    adjustByCal.copy(grain = grainReset.grain)
  }

  def at(h: Hint): TimeData = this.copy(hint = h)

  def resetGrain(r: Grain): TimeData = this.copy(reset = (r, 0))

  override def toString: String = {
    val sb = new StringBuilder(s"{pred = $timePred, grain = $timeGrain")
    if (latent) sb.append(", latent = true")
    if (notImmediate) sb.append(", notImmediate = true")
    for (f <- form) sb.append(s", form = $f")
    for (f <- direction) sb.append(s", direction = $f")
    if (okForThisNext) sb.append(s", okForThisNext = $okForThisNext")
    for (f <- holiday) sb.append(s", holiday = $f")
    for (f <- reset) sb.append(s", reset = $f")
    for (f <- calendar) {
      f match {
        case l: Lunar => sb.append(s", lunar(leap=${l.isLeapMonth}")
        case _        =>
      }
    }
    if (hint != Hint.NoHint) sb.append(s", hint = $hint")
    sb.append("}")
    sb.toString()
  }
}

case class TimeValue(timeValue: SingleTimeValue,
                     tzSeries: List[SingleTimeValue] = Nil,
                     holiday: Option[String] = None,
                     simple: Option[(String, Boolean)] = None,
                     partOfDay: Option[String] = None,
                     override val schema: Option[String] = None)
    extends ResolvedValue {
  override def toString: String = {
    timeValue match {
      case IntervalValue(start, end) => "[%s, %s)".format(start.datetime, end.datetime)
      case SimpleValue(instant)      => instant.datetime.toString
      case OpenIntervalValue(instant, direction) =>
        direction match {
          case IntervalDirection.Before => "(_,%s]".format(instant.datetime)
          case IntervalDirection.After => "[%s,_)".format(instant.datetime)
        }
    }
  }
}
