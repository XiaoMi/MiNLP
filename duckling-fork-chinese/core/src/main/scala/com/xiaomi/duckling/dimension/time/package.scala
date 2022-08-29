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

package com.xiaomi.duckling.dimension

import com.github.heqiao2010.lunar.{LunarCalendar, LunarData}

import java.time.LocalTime

import com.xiaomi.duckling.Types.{ZoneCN, conf}
import com.xiaomi.duckling.dimension.time.Types.{TimeContext, TimeObject, _}
import com.xiaomi.duckling.dimension.time.enums.AMPM._
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums._
import com.xiaomi.duckling.dimension.time.helper.TimeObjectHelpers._
import com.xiaomi.duckling.dimension.time.predicates._

package object time {
  type PastFutureTime = (Stream[TimeObject], Stream[TimeObject])

  private val MaxIter = conf.getInt("dimension.time.max.iter")

  /**
    * Return a tuple of (past, future) elements
    */
  type SeriesPredicateF = (TimeObject, TimeContext) => PastFutureTime

  implicit class GrainWrapper(grain: Grain) {
    def <(that: Grain): Boolean = grain.compareTo(that) < 0

    def <=(that: Grain): Boolean = grain.compareTo(that) <= 0

    def >(that: Grain): Boolean = grain.compareTo(that) > 0

    def >=(that: Grain): Boolean = grain.compareTo(that) >= 0
  }

  private val LunarRefMax = {
    val max = new LunarCalendar(LunarData.MAX_YEAR, 1, 1, false)
    val zero = LocalTime.of(0, 0)
    val _max = DuckDateTime(LunarDate(max), zero, ZoneCN)
    TimeObject(_max, Grain.Second)
  }

  private val LunarRefMin = {
    val min = new LunarCalendar(LunarData.MINI_YEAR, 1, 1, false)
    val zero = LocalTime.of(0, 0)
    val _min = DuckDateTime(LunarDate(min), zero, ZoneCN)
    TimeObject(_min, Grain.Second)
  }

  /**
   * 按历法调整参考时间范围，农历1850~2150，上下2000年
   */
  def refTimeContext(refTime: TimeObject, reverseTake: Boolean = false): TimeContext = {
    val (refMax, refMin) =
      refTime.start.date match {
        case LunarDate(_) => (LunarRefMax, LunarRefMin)
        case SolarDate(_) => (timePlus(refTime, Grain.Year, 2000), timePlus(refTime, Grain.Year, -2000))
      }

    TimeContext(
      refTime = refTime,
      maxTime = refMax,
      minTime = refMin,
      reverseTake = reverseTake
    )
  }

  def resolveTimeData(refTime: TimeObject,
                      td: TimeData,
                      reverseTake: Boolean = false): Option[TimeObject] = {

    val tc = refTimeContext(refTime, reverseTake)

    val (past, future) = runPredicate(td.timePred)(refTime, tc)

    val reverse = if (reverseTake) {
      future match {
        case ahead #:: _ =>
          val happened =
            td.timePred match {
              case _: TimeDatePredicate => timeBefore(ahead, refTime, td.timeGrain)
              case _ => false
            }
          if (happened) Some(ahead)
          else past.headOption
        case _ => past.headOption
      }
    } else None

    if (reverse.nonEmpty) reverse
    // 目前在农历与公历参考时间生成未来过去的序列上还有待梳理，在这里做事后补救
    else if (past.headOption.exists(timeBefore(refTime, _, td.timeGrain))) {
      past.headOption
    } else {
      future match {
        case Stream.Empty => past.headOption
        case ahead #:: nextAhead #:: _ =>
          val happened =
            td.timePred match {
              case _: TimeDatePredicate | _: TimeIntervalsPredicate => timeBefore(ahead, refTime, td.timeGrain)
              case _ => false
            }
          if (happened || td.notImmediate && timeIntersect(ahead)(refTime).nonEmpty) {
            Some(nextAhead)
          } else Some(ahead)
        case ahead #:: _ => Some(ahead)
      }
    }
  }

  val EmptySeries: PastFutureTime = (Stream.empty, Stream.empty)
  val EmptySeriesPredicate: SeriesPredicateF = (_: TimeObject, _: TimeContext) => EmptySeries

  def runPredicate(tp: TimePredicate): SeriesPredicateF = {
    tp match {
      case EmptyTimePredicate => EmptySeriesPredicate
      case SeriesPredicate(f) => f
      case TimeDatePredicate(sec, min, hour, ampm, dayOfWeek, dayOfMonth, month, year, calendar) =>
        if (hour.isEmpty && ampm.nonEmpty) EmptySeriesPredicate
        else {
          val toCompose: List[SeriesPredicateF] = List(
            sec.map(runSecondPredicate),
            min.map(runMinutePredicate),
            hour.map(runHourPredicate(ampm)),
            dayOfWeek.map(runDayOfTheWeekPredicate),
            dayOfMonth.map(runDayOfTheMonthPredicate),
            month.map(runMonthPredicate(calendar)),
            year.map(runYearPredicate)
          ).flatten

          def series(t: TimeObject, tc: TimeContext): PastFutureTime = {
            val pred = toCompose.reduceOption(runCompose).getOrElse(EmptySeriesPredicate)
            val (past, future) = pred(t, tc)
            (past, future)
          }

          series
        }
      case IntersectTimePredicate(pred1, pred2) => runIntersectPredicate(pred1, pred2)
      case TimeIntervalsPredicate(intervalType, p1, p2) =>
        runTimeIntervalsPredicate(intervalType, p1, p2)
      case TimeOpenIntervalPredicate(t) => runTimeOpenIntervalPredicate(t)
      case SequencePredicate(xs)               => runSequencePredicate(xs)
      case ReplacePartPredicate(td1, td2)      => runReplacePartPredicate(td1, td2)
      case EndOfGrainPredicate                 => runEndOfGrainPredicate
      case CycleSeriesPredicate(seriesF, _, _) => seriesF
    }
  }

  def runEndOfGrainPredicate(t: TimeObject, context: TimeContext): PastFutureTime = {
    val (start, grain) = t.grain match {
      case Grain.Month =>
        (t.start.plusMonths(1).plusDays(-1), Day)
      case Grain.Year =>
        (t.start.lastDayOfYear, Day)
      case _ => throw new NotImplementedError(s"the end of ${t.grain}")
    }
    (Stream.empty, Stream(t.copy(start = start, grain = grain)))
  }

  def runReplacePartPredicate(
    td1: TimeData,
    td2: TimeData
  )(t: TimeObject, context: TimeContext): PastFutureTime = {
    (for {
      t1 <- resolveTimeData(t, td1)
      t2 <- resolveTimeData(t, td2)
    } yield {
      val to = (td1.timeGrain, td2.timeGrain) match {
        case (Year, Month) => t2.copy(start = t2.start.withYear(t1.start.year))
        case (Year, Day)   => t2.copy(start = t2.start.withYear(t1.start.year))
        case (Month, Day) =>
          t2.copy(start = t2.start.withMonth(t1.start.month).withYear(t1.start.year))
        case (Day, NoGrain) => // 今天现在
          t2.copy(
            start = t2.start
              .withYear(t1.start.year)
              .withMonth(t1.start.month)
              .withDayOfMonth(t1.start.dayOfMonth)
          )
        case _ => null
      }
      if (to == null) EmptySeries
      else if (to.start.isBefore(t.start)) (Stream(to), Stream.empty)
      else (Stream.empty, Stream(to))
    }).getOrElse(EmptySeries)
  }

  @scala.annotation.tailrec
  def runSequencePredicate(list: List[TimeData])(t: TimeObject,
                                                 context: TimeContext): PastFutureTime = {
    list match {
      case Nil => (Stream.empty, Stream(context.refTime))
      case td :: xs =>
        resolveTimeData(t, td) match {
          case Some(refTime) =>
            val tc = refTimeContext(refTime)
            runSequencePredicate(xs)(refTime, tc)
          case None => EmptySeries
        }
    }
  }

  def runIntersectPredicate(pred1: TimePredicate, pred2: TimePredicate): SeriesPredicateF = {
    runCompose(runPredicate(pred1), runPredicate(pred2))
  }

  def runSecondPredicate(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {
    val s = t.start.second
    val anchor = timePlus(timeRound(t, Second), Second, n - s % 60)
    timeSequence(Minute, 1, anchor)
  }

  def runMinutePredicate(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {
    val rounded = timeRound(t, Minute)
    val m = t.start.minute
    val anchor = timePlus(rounded, Minute, (n - m) % 60)
    timeSequence(Hour, 1, anchor)
  }

  def runHourPredicate(
    ampm: Option[AMPM]
  )(hour: (Boolean, Int))(t: TimeObject, context: TimeContext): PastFutureTime = {
    val (is12H, n) = hour
    val step = if (is12H && n <= 12 && ampm.isEmpty) 12 else 24
    val nAdjust = ampm match {
      case Some(AM) => n % 12
      case Some(PM) => n % 12 + 12
      case _        => n
    }
    val rounded = timeRound(t, Hour)
    val h =
      // 应对今天晚上12点的情况
      if (nAdjust == 24 && t.start.hour == 0) 24
      else if (nAdjust == 12 && t.start.hour == 0) 12
      else Math.floorMod(nAdjust - t.start.hour, step)
    val anchor = timePlus(rounded, Hour, h)
    (
      Stream.iterate(anchor, MaxIter)(timePlus(_, Hour, -step)).tail,
      Stream.iterate(anchor, MaxIter)(timePlus(_, Hour, step))
    )
  }

  def runDayOfTheWeekPredicate(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {
    val daysUntilNextWeek = Math.floorMod(n - t.start.dayOfWeek, 7)
    val anchor = timePlus(timeRound(t, Day), Day, daysUntilNextWeek)
    timeSequence(Day, 7, anchor)
  }

  def runDayOfTheMonthPredicate(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {

    def enoughDays(t: TimeObject): Boolean = {
      n <= t.start.date.lengthOfMonth
    }

    def addDays(t: TimeObject): TimeObject = timePlus(t, Day, n - 1)

    def addMonth(i: Int)(t: TimeObject): TimeObject = timePlus(t, Month, i)

    def roundMonth(t: TimeObject): TimeObject = timeRound(t, Month)

    val rounded = roundMonth(t)
    val anchor = if (t.start.dayOfMonth <= n) rounded else addMonth(1)(rounded)
    val past =
      Stream.iterate(addMonth(-1)(anchor), MaxIter)(addMonth(-1)).filter(enoughDays).map(addDays)
    val future = Stream.iterate(anchor, MaxIter)(addMonth(1)).filter(enoughDays).map(addDays)
    (past, future)
  }

  def runMonthPredicate(calendar: Option[Calendar])(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {
    val y = timeRound(t, Year)
    val rounded =
      calendar match {
        case Some(Solar) | None => timePlus(y, Month, n - 1)
        case Some(Lunar(_)) => timeWith(y, Month, n)
      }

    val anchor =
      if (timeStartsBeforeTheEndOf(t)(rounded)) rounded else timePlus(rounded, Year, 1)
    timeSequence(Year, 1, anchor)
  }

  def runYearPredicate(n: Int)(t: TimeObject, context: TimeContext): PastFutureTime = {
    val year = n
    val tyear = t.start.year
    val y = timePlus(timeRound(t, Year), Year, year - tyear)
    if (tyear <= year) (Stream.empty, Stream(y))
    else (Stream(y), Stream.empty)
  }

  // Limits how deep into lists of segments to look
  val safeMax = 10

  /**
    * Performs best when pred1 is smaller grain than pred2
    */
  def runCompose(pred1: SeriesPredicateF, pred2: SeriesPredicateF): SeriesPredicateF = {
    val series = (nowTime: TimeObject, context: TimeContext) => {
      val (past, future) = pred2(nowTime, context)

      def startsBefore(t1: TimeObject)(t: TimeObject): Boolean = timeStartsBeforeTheEndOf(t)(t1)

      def computeSeries(tokens: Stream[TimeObject]): Stream[TimeObject] = {
        tokens.take(safeMax).flatMap { time1 =>
          val (past, future) = pred1(time1, fixedTimeContext(time1))
          val before = future.takeWhile(startsBefore(time1))
          before.flatMap(timeIntersect(time1))
        }
      }

      val backward = computeSeries(past.takeWhile(timeStartsBeforeTheEndOf(context.minTime)))
      val forward = computeSeries(future.takeWhile(timeStartsBeforeTheEndOf(_)(context.maxTime)))
      (backward, forward)
    }
    series
  }

  def runTimeIntervalsPredicate(intervalType: IntervalType,
                                pred1: TimePredicate,
                                pred2: TimePredicate): SeriesPredicateF = {
    // Pick the first interval *after* the given time segment
    def f(thisSegment: TimeObject, ctx: TimeContext): Option[TimeObject] = {
      runPredicate(pred2)(thisSegment, ctx) match {
        case (_, firstFuture #:: _) =>
          Some(timeInterval(intervalType, thisSegment, firstFuture))
        case _ => None
      }
    }

    timeSeqMap(dontReverse = true, f, pred1)
  }

  /**
    * Limits how deep into lists of segments to look
    */
  val safeMaxInterval = 12

  /**
    * Applies `f` to each interval yielded by `g`.
    * Intervals including "now" are in the future.
    *
    * @param dontReverse
    * @param f Given an interval and range, compute a single new interval
    * @param g First-layer series generator
    * @return Series generator for values that come from `f`
    */
  def timeSeqMap(dontReverse: Boolean,
                 f: (TimeObject, TimeContext) => Option[TimeObject],
                 g: TimePredicate): SeriesPredicateF = {
    def seriesF(nowTime: TimeObject, context: TimeContext) = {
      // computes a single interval from `f` based on each interval in the series
      def applyF(series: Stream[TimeObject]) = {
        series.take(safeMaxInterval).flatMap(f(_, context))
      }

      val (firstPast, firstFuture) = runPredicate(g)(nowTime, context)
      val (past1, future1) = (applyF(firstPast), applyF(firstFuture))

      // Separate what's before and after now from the past's series
      val (newFuture, stillPast) = past1.span(timeStartsBeforeTheEndOf(nowTime))
      // A series that ends at the earliest time
      val oldPast = stillPast.takeWhile(timeStartsBeforeTheEndOf(context.minTime))
      val (newPast, stillFuture) = future1.span(t => !timeStartsBeforeTheEndOf(nowTime)(t))
      // A series that ends at the furthest future time
      val oldFuture = stillFuture.takeWhile(timeStartsBeforeTheEndOf(_)(context.maxTime))

      // Reverse the list if needed?
      def applyRev(series: Stream[TimeObject]) = if (dontReverse) series else series.reverse

      val (sortedPast, sortedFuture) = (applyRev(newPast), applyRev(newFuture))

      // Past is the past from the future's series with the
      // past from the past's series tacked on
      val past = sortedPast ++ oldPast
      // Future is the future from the past's series with the
      // future from the future's series tacked on
      val future = sortedFuture ++ oldFuture
      (past, future)
    }

    seriesF
  }

  def isEmptyPredicate(p: TimePredicate): Boolean = p match {
    case EmptyTimePredicate => true
    case _                  => false
  }

  def runTimeOpenIntervalPredicate(it: IntervalDirection)(t: TimeObject, context: TimeContext): PastFutureTime = {
    (Stream(t.copy(direction = Some(it))), Stream.empty)
  }
}
