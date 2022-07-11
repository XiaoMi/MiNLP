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

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.numeral.Predicates.isIntegerBetween
import com.xiaomi.duckling.dimension.numeral.seq.{DigitSequence, DigitSequenceData}
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.time.date.Date
import com.xiaomi.duckling.dimension.time.duration.{Duration, DurationData}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums._
import com.xiaomi.duckling.dimension.time.form.{Month => _, _}

object predicates {
  trait TimePredicate

  case object EmptyTimePredicate extends TimePredicate

  /**
   * invariant: at least one of them is not None
   */
  case class TimeDatePredicate(second: Option[Int] = None,
                               minute: Option[Int] = None,
                               hour: Option[(Boolean, Int)] = None,
                               ampm: Option[AMPM] = None,
                               dayOfWeek: Option[Int] = None,
                               dayOfMonth: Option[Int] = None,
                               month: Option[Int] = None,
                               year: Option[Int] = None,
                               calendar: Option[Calendar] = None)
    extends TimePredicate {
    override def toString: String = {
      val sb = new StringBuilder("{")
      if (calendar.nonEmpty) sb.append(s"calendar = ${calendar.get}")
      if (year.nonEmpty) sb.append(s"year = ${year.get}, ")
      if (month.nonEmpty) sb.append(s"month = ${month.get}, ")
      if (dayOfMonth.nonEmpty) sb.append(s"day = ${dayOfMonth.get}, ")
      if (dayOfWeek.nonEmpty) sb.append(s"week = ${dayOfWeek.get}, ")
      if (hour.nonEmpty) sb.append(s"hour = ${hour.get}, ")
      if (minute.nonEmpty) sb.append(s"minute = ${minute.get}, ")
      if (second.nonEmpty) sb.append(s"second = ${second.get}, ")
      sb.append("}")
      sb.toString()
    }
  }

  case class SeriesPredicate(f: SeriesPredicateF) extends TimePredicate {
    override def toString: String = "f:Series"
  }

  case class IntersectTimePredicate(pred1: TimePredicate, pred2: TimePredicate)
    extends TimePredicate

  case class TimeIntervalsPredicate(t: IntervalType, p1: TimePredicate, p2: TimePredicate)
    extends TimePredicate

  case class TimeOpenIntervalPredicate(t: IntervalDirection) extends TimePredicate

  case class SequencePredicate(xs: List[TimeData]) extends TimePredicate

  /**
   * 用td1的部分信息替换td2的结果，解决明年的今天、上个月的今天之类的问题
   */
  case class ReplacePartPredicate(td1: TimeData, td2: TimeData) extends TimePredicate

  case object EndOfGrainPredicate extends TimePredicate

  case class CycleSeriesPredicate(seriesF: SeriesPredicateF, n: Int, grain: Grain)
    extends TimePredicate

  val singleNumeber: Predicate = isIntegerBetween(0, 9)

  val seqYearOf1000to9999: Predicate = {
    case Token(DigitSequence, DigitSequenceData(seq, zh, raw)) if seq.length == 4 =>
      val v = seq.toDouble
      v > 1000 && v < 9999
  }

  val arabicSeqOf1950to2050: Predicate = {
    case Token(DigitSequence, DigitSequenceData(seq, zh, raw)) if !zh =>
      val v = seq.toDouble
      v > 1950 && v < 2050
  }

  val cnSeqOf1950to2050: Predicate = {
    case Token(DigitSequence, DigitSequenceData(seq, zh, raw)) if zh =>
      val v = seq.toDouble
      v > 1950 && v < 2050
  }

  def andPredicateCompose(list: List[Predicate]): Predicate = {
    case token: Token => list.exists(_ apply token)
  }

  def orPredicateCompose(list: List[Predicate]): Predicate = {
    case token: Token => list.forall(_ apply token)
  }

  val isAYear: Predicate = {
    case Token(_, td: TimeData) => td.timeGrain == Year
  }

  val isAMonth: Predicate = {
    case Token(_, td: TimeData) => td.timeGrain == Month
  }

  val isADayOfMonth: Predicate = {
    case Token(_, td: TimeData) => td.timeGrain == Day
  }

  val isADayOfWeek: Predicate = {
    case Token(_, td: TimeData) => td.form.contains(DayOfWeek)
  }

  val isAPartOfDay: Predicate = {
    case Token(_, td: TimeData) =>
      td.form match {
        case Some(PartOfDay(_)) => true
        case _                  => false
      }
  }

  val isAnHourOfDay: Predicate = {
    case Token(Time, td: TimeData) =>
      val c1 = td.form match {
        case Some(TimeOfDay(Some(_), _)) => true
        case _                           => false
      }
      c1 || td.timeGrain > Minute
  }

  val isATimeOfDay: Predicate = {
    case Token(Time, td: TimeData) =>
      td.form match {
        case Some(TimeOfDay(_, _)) => true
        case _                     => false
      }
    case _ => false
  }

  val isIntervalOfDay: Predicate = {
    case Token(Time, td: TimeData) =>
      td.form match {
        case Some(IntervalOfDay) => true
        case _                   => false
      }
    case _ => false
  }

  val isWeekend: Predicate = {
    case Token(_, td: TimeData) => td.form.contains(Weekend)
  }

  val isNotLatent: Predicate = {
    case Token(_, td: TimeData) => !td.latent
  }

  val isNumberOrUnitNumber: Predicate = {
    case Token(_, _: NumeralData) => true
    case _                        => false
  }

  val isTimeDatePredicate: Predicate = {
    case Token(_, td: TimeData) =>
      td.timePred match {
        case _: TimeDatePredicate => true
        case _                    => false
      }
  }

  /**
    * 几点几分的说法中：
    * 正确：九点十分
    * 错误：九点十
    *
    * 正确：九点零八/一十/二十/二十三
    */
  val isAMinuteWithoutUnit: Predicate = {
    case td @ Token(Numeral, _)                             => isIntegerBetween(11, 59)(td)
    case Token(DigitSequence, DigitSequenceData(seq, _, _)) => seq.length == 2 && seq(0) == '零'
  }

  val isAQuarterOfYear: Predicate = {
    case Token(Duration, DurationData(value, grain, _, _)) =>
      grain == Quarter && value >= 1 && value <= 4
  }

  val isOkForNext: Predicate = {
    case Token(Time | Date, td: TimeData) => td.okForThisNext
  }

  def isHint(hints: Hint*): Predicate = {
    case Token(Time | Date, td: TimeData) => hints.contains(td.hint)
  }

  val isNotHint: Hint => Predicate = (hint: Hint) => {
    case Token(Time | Date, td: TimeData) => td.hint != hint
  }

  val is24oClockOfDay: Predicate = {
    case Token(Time, td: TimeData) =>
      (td.timePred, td.form) match {
        case (date: TimeDatePredicate, Some(TimeOfDay(_, _))) =>
          date.hour match {
            case Some((_, hour)) => hour == 24
            case None            => false
          }
        case _ => false
      }
  }

  val isDurationAmountGt1: Predicate = {
    case Token(Duration, DurationData(value, _, _, _)) => value > 1
  }

  val isGrainGeDay: Predicate = {
    case Token(Time, td: TimeData) => td.timeGrain >= Grain.Day
  }

  val isLunarHoliday: Predicate = {
    case Token(Time, td: TimeData) =>
      td.holiday.nonEmpty &&
        (td.calendar match {
          case Some(Solar) => false
          case Some(Lunar(_)) => true
          case None => false
        })
  }
}
