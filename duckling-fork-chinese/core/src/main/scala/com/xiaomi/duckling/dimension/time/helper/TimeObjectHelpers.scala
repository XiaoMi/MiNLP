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

package com.xiaomi.duckling.dimension.time.helper

import com.github.heqiao2010.lunar.LunarCalendar

import java.time.{LocalTime, ZonedDateTime}
import java.util.{Calendar => JCalendar}
import scala.annotation.tailrec

import com.xiaomi.duckling.dimension.time.GrainWrapper
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.IntervalType._
import com.xiaomi.duckling.dimension.time.enums._

object TimeObjectHelpers {

  def timeRound(t: TimeObject, grain: Grain): TimeObject = {
    val zone = t.start.zone
    val d = t.start
    val (year, month, dayOfMonth, hours, mins, secs) =
      (d.year, d.month, d.dayOfMonth, d.hour, d.minute, d.second)
    val newMonth = if (grain > Month) 1 else month
    val newDayOfMonth = if (grain > Week) 1 else dayOfMonth
    val newHours = if (grain > Hour) 0 else hours
    val newMins = if (grain > Minute) 0 else mins
    val newSecs = if (grain > Second) 0 else secs
    val time = LocalTime.of(newHours, newMins, newSecs)
    val date = t.start.date.of(year, newMonth, newDayOfMonth)
    val datetime = DuckDateTime(date, time, zone)
    val (start, g) =
      if (grain == Week) (datetime.minusDays(datetime.dayOfWeek - 1), Week)
      else (datetime, grain)
    TimeObject(start = start, grain = g)
  }

  def timeSequence(grain: Grain,
                   step: Int,
                   anchor: TimeObject): (Stream[TimeObject], Stream[TimeObject]) = {
    def f(n: Int)(t: TimeObject): TimeObject = timePlus(t, grain, n)

    (Stream.iterate(anchor)(f(-step)).tail, Stream.iterate(anchor)(f(step)))
  }

  def timePlus(t: TimeObject, theGrain: Grain, n: Int): TimeObject = {
    TimeObject(
      start = add(t.start, theGrain, n),
      grain = if (theGrain > t.grain) t.grain else theGrain
    )
  }

  def add(dateTime: DuckDateTime, grain: Grain, n: Int): DuckDateTime = {
    grain match {
      case NoGrain => dateTime.plusSeconds(n)
      case Second  => dateTime.plusSeconds(n)
      case Minute  => dateTime.plusMinutes(n)
      case Hour    => dateTime.plusHours(n)
      case Day     => dateTime.plusDays(n)
      case Week    => dateTime.plusWeeks(n)
      case Month   => dateTime.plusMonths(n)
      case Year    => dateTime.plusYears(n)
      case Quarter => dateTime.plusMonths(3)
    }
  }

  def timeWith(t: TimeObject, theGrain: Grain, n: Int): TimeObject = {
    TimeObject(
      start = `with`(t.start, theGrain, n),
      grain = if (theGrain > t.grain) t.grain else theGrain
    )
  }

  /**
   * 闰月时，不可用累加数出月份来
   */
  def `with`(dateTime: DuckDateTime, grain: Grain, n: Int): DuckDateTime = {
    grain match {
      case Month => dateTime.date match {
        case d1@LunarDate(_) => dateTime.withMonth(n)
        case SolarDate(d2) =>
          val lunar = new LunarCalendar(d2.getYear, n, d2.getDayOfMonth, false)
          dateTime.copy(date = LunarDate(lunar))
      }
      case _ => add(dateTime, grain, n - 1)
    }
  }

  /**
   * 时间求交，比如将 date 部分和 time 部分合并，注意，需要先让历法一致
   * @param t1
   * @param t2
   * @return
   */
  @tailrec
  def timeIntersect(t1: TimeObject)(t2: TimeObject): Option[TimeObject] = {
    (t1, t2) match {
      case (TimeObject(start1, g1, end1), TimeObject(start2, g2, _)) =>
        val e1 = timeEnd(t1)
        val e2 = timeEnd(t2)
        val s1 = timeStart(t1)
        val s2 = timeStart(t2)
        val gg = if (g1 > g2) g2 else g1
        if (s1.isAfter(s2)) timeIntersect(t2)(t1)
        else if (!e1.isAfter(s2)) None
        else if (e1.isBefore(e2) || s1 == s2 && e1 == e2 && end1.nonEmpty) {
          Some(TimeObject(start = s2, end = end1, grain = gg))
        } else Some(t2.copy(grain = gg))
    }
  }

  def timeValue(t: TimeObject): SingleTimeValue = {
    t match {
      case TimeObject(s, g, None) => SimpleValue(InstantValue(s, g))
      case TimeObject(s, g, Some(e)) => IntervalValue(InstantValue(s, g), InstantValue(e, g))
    }
  }

  @inline
  def timeEnd(t: TimeObject): DuckDateTime = t.end.getOrElse(add(t.start, t.grain, 1))

  @inline
  def timeStart(t: TimeObject): DuckDateTime = t.start

  def timeStartsBeforeTheEndOf(t1: TimeObject)(t2: TimeObject): Boolean = {
    t1.start.isBefore(timeEnd(t2))
  }

  @tailrec
  def timeBefore(t1: TimeObject, t2: TimeObject, grain: Grain = NoGrain): Boolean = {
    if (grain == NoGrain) t1.start.isBefore(t2.start)
    else timeBefore(timeRound(t1, grain), timeRound(t2, grain))
  }

  def timeInterval(intervalType: IntervalType, to1: TimeObject, to2: TimeObject): TimeObject = {
    val TimeObject(s1, g1, _) = to1
    val TimeObject(s2, g2, e2) = to2
    val g11 = if (g1 < g2) g1 else g2
    val g22 =
      if (g1 < Grain.Day && g2 < Grain.Day) g11
      else g2
    val end = intervalType match {
      case Open => s2
      case Closed => e2.getOrElse(add(s2, g22, 1))
    }
    TimeObject(s1, g11, Some(end))
  }

  def toSolar(t: ZonedDateTime, isLeapMonth: Boolean): ZonedDateTime = {
    val y = t.getYear
    val m = t.getMonthValue
    val d = t.getDayOfMonth
    val cal = LunarCalendar.lunar2Solar(y, m, d, isLeapMonth)
    t.withYear(cal.get(JCalendar.YEAR))
      .withMonth(cal.get(JCalendar.MONTH) + 1)
      .withDayOfMonth(cal.get(JCalendar.DAY_OF_MONTH))
  }
}
