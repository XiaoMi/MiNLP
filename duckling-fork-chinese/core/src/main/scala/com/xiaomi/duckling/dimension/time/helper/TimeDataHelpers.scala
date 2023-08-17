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

import java.time.{DayOfWeek => JDayOfWork}

import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.enums.AMPM.{AM, PM}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.IntervalType._
import com.xiaomi.duckling.dimension.time.enums.{Calendar, Grain, Hint, IntervalType, _}
import com.xiaomi.duckling.dimension.time.form.{DayOfWeek, Form, Month => _, _}
import com.xiaomi.duckling.dimension.time.helper.TimePredicateHelpers._
import com.xiaomi.duckling.dimension.time.predicates.{EmptyTimePredicate, TimeDatePredicate}
import com.xiaomi.duckling.dimension.time.{TimeData, _}

object TimeDataHelpers {

  /**
    * n个grain的时间区间
    */
  def cycleN(notImmediate: Boolean, grain: Grain, n: Int): TimeData = {
    cycleN(notImmediate, grain, n, grain)
  }

  def cycleN(notImmediate: Boolean, grain: Grain, n: Int, roundGrain: Grain): TimeData = {
    TimeData(
      timePred = takeN(n, notImmediate, timeCycle(grain, roundGrain)),
      timeGrain = grain,
      notImmediate = notImmediate
    )
  }

  /**
    * 第n个grain粒度的时间点
    */
  def cycleNth(grain: Grain, n: Int): TimeData = cycleNth(grain, n, grain)

  def cycleNth(grain: Grain, n: Int, roundGrain: Grain): TimeData = {
    val pred =
      takeNth(1, notImmediate = false, timeCycle(grain, roundGrain, step = n))
    TimeData(pred, timeGrain = grain)
  }

  /**
   * 这/今/本/下/... ruleRecentCycle中的组合并非都是符合习惯的，细化
   *
   * @param g Grain列表
   * @param n cycleNth(g, n)
   * @return
   */
  def cycleNthThis(n: Int, g: Grain, grains: Grain*): Option[TimeData] = {
    if (grains.isEmpty) cycleNth(g, n)
    else grains.find(_ == g).map(_ => cycleNth(g, n))
  }

  /**
    * Generalized version of cycleNth with custom predicate
    */
  val predNth = (n: Int, notImmediate: Boolean) =>
    (td: TimeData) => {
      TimeData(
        timePred = takeNth(n, notImmediate, td.timePred),
        timeGrain = td.timeGrain,
        notImmediate = notImmediate,
        holiday = td.holiday
      )
  }

  // TimeData操作函数，方便组合使用
  def form(f: Form, td: TimeData): TimeData = td.copy(form = f)

  val mkLatent = (td: TimeData) => td.copy(latent = true)

  val partOfDay = (part: String, td: TimeData) => form(PartOfDay(part), td)

  def reset(r: Grain)(td: TimeData): TimeData = td.copy(reset = (r, 0))

  def timeOfDay(h: Option[Int], is12H: Boolean, td: TimeData): TimeData = {
    form(TimeOfDay(h, is12H), td)
  }

  def now: TimeData =
    cycleNth(Second, 0, Second).copy(timeGrain = NoGrain).at(Hint.RecentNominal)

  def today: TimeData = cycleNth(Day, 0).at(Hint.RecentNominal)

  def latentYear(n: Int): TimeData = {
    year(n).copy(latent = true)
  }

  def weekend(beforeEndOfInterval: Boolean): TimeData = form(Weekend, interval1(Open, dayOfWeek(6), dayOfWeek(7), beforeEndOfInterval))

  def timeOfDayAMPM(isAM: Boolean, td: TimeData): TimeData = {
    val ampmPred: TimeDatePredicate = TimeDatePredicate(ampm = if (isAM) AM else PM)
    val ampm = TimeData(timePred = ampmPred, timeGrain = Hour)
    val td1 = intersect1(td, ampm)
    timeOfDay(None, is12H = false, td1)
  }

  // 构造 TimeDatePredicate

  val year = (y: Int) => {
    TimeData(timePred = timeYear(y), timeGrain = Year)
  }

  val month = (m: Int) => {
    TimeData(timePred = timeMonth(m), timeGrain = Month).at(Hint.MonthOnly)
  }

  def dayOfMonth(d: Int): TimeData =
    TimeData(timeDayOfMonth(d), timeGrain = Day).at(Hint.DayOnly)

  def hour(is12H: Boolean, n: Int): TimeData = {
    val td = TimeData(timePred = timeHour(is12H, n), timeGrain = Hour)
    timeOfDay(Some(n), is12H, td)
  }

  def minute(n: Int): TimeData = {
    TimeData(timePred = timeMinute(n), timeGrain = Minute)
  }

  def second(n: Int): TimeData = TimeData(timePred = timeSecond(n), timeGrain = Second)

  def hourMinuteSecond(is12H: Boolean, h: Int, m: Int, s: Option[Int] = None): TimeData = {
    val td = s match {
      case Some(v) => intersect1(hour(is12H, h), intersect1(minute(m), second(v)))
      case None => intersect1(hour(is12H, h), minute(m))
    }

    timeOfDay(Some(h), is12H, td)
  }

  def yearMonth(y: Int, m: Int): TimeData = intersect1(year(y), month(m))

  def yearMonthDay(y: Int, m: Int, d: Int): TimeData = intersect1(yearMonth(y, m), dayOfMonth(d))

  def monthDay(m: Int, d: Int): TimeData = intersect1(month(m), dayOfMonth(d))

  def dayOfWeek(n: Int): TimeData = {
    val td = TimeData(timePred = timeDayOfWeek(n), timeGrain = Day, notImmediate = true)
    form(DayOfWeek, td)
  }

  def lunar(td: TimeData, isLeapMonth: Boolean = false): TimeData = {
    val pred = td.timePred match {
      case t: TimeDatePredicate => t.copy(calendar = Lunar(isLeapMonth))
      case _ =>
        throw new IllegalAccessError(
          s"lunar support TimeDatePredicate only but meet ${td.timePred.getClass.getSimpleName}"
        )
    }
    td.copy(timePred = pred, calendar = Lunar(isLeapMonth))
  }

  def intersect(td1: TimeData, td2: TimeData): Option[TimeData] = {
    // 避免无效组合
    if (td1.hint == Hint.FinalRule || td2.hint == Hint.FinalRule) None
    else {
      val td = intersect1(td1, td2)
      if (td.timePred == EmptyTimePredicate) None
      else {
        if (td1.reset.nonEmpty && td2.reset.isEmpty) td.copy(reset = td1.reset)
        else if (td2.reset.nonEmpty && td1.reset.isEmpty) td.copy(reset = td2.reset)
        // reset不一致暂不处理
        else td
      }
    }
  }

  def intersect1(t1: TimeData, t2: TimeData): TimeData = {
    val direction = t1.direction orElse t2.direction
    val holiday = t1.holiday orElse t2.holiday
    val calendar = (t1.calendar, t2.calendar) match {
      case (Some(Lunar(_)), _) => t1.calendar
      case (_, Some(Lunar(_))) => t2.calendar
      case (Some(_), _) => t1.calendar
      case _ => t2.calendar
    }
    if (t1.timeGrain < t2.timeGrain) {
      TimeData(
        timePred = timeCompose(t1.timePred, t2.timePred),
        timeGrain = t1.timeGrain,
        holiday = holiday,
        direction = direction,
        calendar = calendar
      )
    } else {
      TimeData(
        timePred = timeCompose(t2.timePred, t1.timePred),
        timeGrain = t2.timeGrain,
        holiday = holiday,
        direction = direction,
        calendar = calendar
      )
    }
  }

  /**
    * Generalized version of `cycleNthAfter` with custom predicate
    */
  def predNthAfter(n: Int, td: TimeData, base: TimeData): TimeData = {
    TimeData(takeNthAfter(n, true, td.timePred, base.timePred), timeGrain = td.timeGrain)
  }

  /**
    * Zero-indexed weeks, Monday is 1
    * Use `predLastOf` for last day of week of month
    *
    * @param dow day of week
    * @return
    */
  def nthDayOfWeekOfMonth(m: Int, nth: Int, dow: JDayOfWork): TimeData = {
    val dow_ = dayOfWeek(dow.getValue)
    val month_ = month(m)
    predNthAfter(nth - 1, dow_, month_)
  }

  def interval(intervalType: IntervalType, td1: TimeData, td2: TimeData, beforeEndOfInterval: Boolean): Option[TimeData] = {
    interval1(intervalType, td1, td2, beforeEndOfInterval) match {
      case td: TimeData if td.timePred == EmptyTimePredicate => None
      case res                                               => Some(res)
    }
  }

  def interval1(intervalType: IntervalType, td1: TimeData, td2: TimeData, beforeEndOfInterval: Boolean): TimeData = {
    val it =
      if (td1.hint == Hint.Season && td2.hint == Hint.Season) intervalType
      else if (td1.timeGrain == td2.timeGrain && td1.timeGrain == Day) Closed
      else intervalType
    TimeData(
      timePred = mkTimeIntervalsPredicate(it, td1, td2, beforeEndOfInterval),
      timeGrain = if (td1.timeGrain > td2.timeGrain) td2.timeGrain else td1.timeGrain
    )
  }

  def calendar(td1: TimeData, td2: TimeData): Option[Calendar] = {
    (td1.calendar, td2.calendar) match {
      case (None, _) => td2.calendar
      case (_, None) => td1.calendar
      case (Some(c1), Some(c2)) if c1 == c2 => Some(c1)
      case _ => None // 不一致的先搁置
    }
  }

  def finalRule(td: TimeData): TimeData = td.copy(hint = Hint.FinalRule)
}
