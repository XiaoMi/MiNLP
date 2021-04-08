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

package duckling.dimension.time

import com.typesafe.scalalogging.LazyLogging
import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.Dimension
import duckling.dimension.numeral.Numeral
import duckling.dimension.numeral.Predicates.isNatural
import duckling.dimension.numeral.seq.DigitSequence
import duckling.dimension.ordinal.Ordinal
import duckling.dimension.time.Types._
import duckling.dimension.time.date.Date
import duckling.dimension.time.duration.Duration
import duckling.dimension.time.enums._
import duckling.dimension.time.form.{Form, PartOfDay, TimeOfDay}
import duckling.dimension.time.grain.TimeGrain
import duckling.dimension.time.helper.TimeObjectHelpers._
import duckling.dimension.time.predicates.{IntersectTimePredicate, SequencePredicate, TimeDatePredicate, TimePredicate}
import duckling.dimension.time.unitNumber.UnitNumber
import duckling.exceptions.LunarOutOfRangeException
import duckling.ranking.Types.{DiscreteFeature, Feature}
import duckling.types.Node

case object Time extends Dimension with Rules with Examples {
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
  *
  * @param resetTimeOfDay 上午是否总是需要指今天的上午，默认是未来一个上午
  * @param recentInFuture 最近是向前计算还是向后计算，默认是未来
	* @param alwaysInFuture 对于过去的日期是否取未来一年的日期
  */
case class TimeOptions(resetTimeOfDay: Boolean = false, recentInFuture: Boolean = true, alwaysInFuture: Boolean = true)

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
                    hint: Hint = Hint.NoHint // 一些提示信息，比如不能够再组合，已经进行过时间组合（去年的今天）之类的
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
            simple = generateSimple(timePred),
            holiday = holiday,
            partOfDay = partOfDay
          )
        Some(tv, latent)
      } catch {
        case e: LunarOutOfRangeException =>
          logger.warn(e.getMessage)
          None
      }

    }
  }

  def generateSimple(timePred: TimePredicate): Option[String] = {
    timePred match {
      case TimeDatePredicate(_, _, _, _, _, dayOfMonth, month, year, _) =>
        val y = year.map(_.toString).getOrElse("x")
        val (m, d) = timeGrain match {
          case Grain.Year => ("x", "x")
          case Grain.Month =>
            month.map(s => (s.toString, "x")).getOrElse(("-", "-"))
          case Grain.Day =>
            val m = month.map(_.toString).getOrElse("x")
            val d = dayOfMonth.map(_.toString).getOrElse("x")
            (m, d)
          case _ => ("-", "-")
        }
        if (y == m && m == d && d == "x") None
        else if (m == "-" && d == "-") None
        else Some(s"$y-$m-$d")
      case IntersectTimePredicate(SequencePredicate(_), TimeDatePredicate(_, _, _, _, _, _, _, year, _)) =>
        val y = year.map(_.toString).getOrElse("x") // 如：2021年除夕， 除夕需要根据农历最后一天计算具体日期，只抽取年
      val (m, d) = ("x", "x")
        if (y == m && m == d && d == "x") None
        else if (m == "-" && d == "-") None
        else Some(s"$y-$m-$d")
      case _ => None
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
                     simple: Option[String] = None,
                     partOfDay: Option[String] = None)
    extends ResolvedValue {
  override def toString: String = {
    timeValue match {
      case IntervalValue(start, end) => "[%s, %s)".format(start.datetime, end.datetime)
      case SimpleValue(instant)      => instant.datetime.toString
      case _                         => super.toString
    }
  }
}
