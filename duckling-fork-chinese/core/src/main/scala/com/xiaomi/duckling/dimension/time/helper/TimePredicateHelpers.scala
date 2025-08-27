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

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time._
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.IntervalType._
import com.xiaomi.duckling.dimension.time.enums.{Grain, IntervalType}
import com.xiaomi.duckling.dimension.time.form.{PartOfDay, Month => _}
import com.xiaomi.duckling.dimension.time.helper.TimeObjectHelpers._
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.rule.SolarTerms.solarTermTable

object TimePredicateHelpers {

  /**
    * Time predicate constructors
    */
  def mkIntersectPredicate(tp1: TimePredicate, tp2: TimePredicate): TimePredicate = {
    if (tp1 == EmptyTimePredicate || tp2 == EmptyTimePredicate) EmptyTimePredicate
    else {
      def unify[T](a: Option[T], b: Option[T]): Option[T] = {
        (a, b) match {
          case (None, b1) => b1
          case (a1, None) => a1
          case (Some(c1), Some(d1)) =>
            if (c1 == d1) Some(c1)
            else throw new IllegalArgumentException("cannot merge")
        }
      }

      (tp1, tp2) match {
        case (
            TimeDatePredicate(a1, b1, c1, d1, e1, f1, g1, h1, i1),
            TimeDatePredicate(a2, b2, c2, d2, e2, f2, g2, h2, i2)
            ) =>
          try {
            TimeDatePredicate(
              unify(a1, a2),
              unify(b1, b2),
              unify(c1, c2),
              unify(d1, d2),
              unify(e1, e2),
              unify(f1, f2),
              unify(g1, g2),
              unify(h1, h2),
              unify(i1, i2)
            )
          } catch {
            case _: IllegalArgumentException => EmptyTimePredicate
          }

        case _ => IntersectTimePredicate(tp1, tp2)
      }
    }
  }

  // 构造TimeDatePredicate

  def timeYear(n: Int): TimePredicate = TimeDatePredicate(year = Some(n))

  def timeMonth(n: Int): TimePredicate = TimeDatePredicate(month = Some(n))

  def timeMonthDay(m: Int, d: Int): TimePredicate = TimeDatePredicate(month = m, dayOfMonth = d)

  def timeHour(is12H: Boolean, n: Int): TimePredicate = TimeDatePredicate(hour = (is12H, n))

  def timeMinute(n: Int): TimePredicate = TimeDatePredicate(minute = n)

  def timeSecond(n: Int): TimePredicate = TimeDatePredicate(second = n)

  def timeDayOfMonth(i: Int): TimePredicate = TimeDatePredicate(dayOfMonth = Some(i))

  def timeDayOfWeek(i: Int): TimePredicate = TimeDatePredicate(dayOfWeek = Some(i))

  def mkTimeIntervalsPredicate(t: IntervalType,
                               ta: TimeData,
                               tb: TimeData,
                               beforeEndOfInterval: Boolean): TimePredicate = {
    val a = ta.timePred
    val b = tb.timePred

    if (a == EmptyTimePredicate || b == EmptyTimePredicate) EmptyTimePredicate
    // `from (... from a to b ...) to c` and `from c to (... from a to b ...)` don't
    // really have a good interpretation, so abort early
    // 允许上午到下午作为区间
    else if (containsTimeIntervalsPredicate(a) && !isPartOfDay(ta) || containsTimeIntervalsPredicate(b) && !isPartOfDay(tb)) {
      EmptyTimePredicate
    }
    // this is potentially quadratic, but the sizes involved should be small
    else TimeIntervalsPredicate(t, a, b, beforeEndOfInterval)
  }

  def isPartOfDay(td: TimeData): Boolean = {
    td.form match {
      case Some(PartOfDay(_)) => true
      case _ => false
    }
  }

  def containsTimeIntervalsPredicate(predicate: TimePredicate): Boolean = {
    predicate match {
      case _: TimeIntervalsPredicate => true
      case IntersectTimePredicate(a, b) =>
        containsTimeIntervalsPredicate(a) || containsTimeIntervalsPredicate(b)
      case _ => false
    }
  }

  /**
    * Like `takeNth`, but takes the nth cyclic predicate after `basePred`
    */
  def takeNthAfter(n: Int,
                   notImmediate: Boolean,
                   cyclicPred: TimePredicate,
                   basePred: TimePredicate): TimePredicate = {
    def f(t: TimeObject, ctx: TimeContext): Option[TimeObject] = {
      val (past, future) = runPredicate(cyclicPred)(t, ctx)
      val rest = if (n >= 0) {
        future match {
          case ahead #:: _ if notImmediate && timeBefore(ahead, t) => future.drop(n + 1)
          case _                                                   => future.drop(n)
        }
      } else past.drop(-(n + 1))
      rest match {
        case nth #:: _ => nth
        case _         => None
      }
    }

    SeriesPredicate(timeSeqMap(false, f, basePred), basePred.maxGrain)
  }

  def timeCycle(grain: Grain): CycleSeriesPredicate = timeCycle(grain, grain)

  def timeCycle(grain: Grain, roundGrain: Grain, step: Int = 1): CycleSeriesPredicate = {
    CycleSeriesPredicate((t: TimeObject, _: TimeContext) => {
      timeSequence(grain, step, if (roundGrain != NoGrain) timeRound(t, roundGrain) else t)
    }, step, grain)
  }

  /**
    * Takes `n` cycles of `f`
    */
  def takeN(literalN: Int, notImmediate: Boolean, cycleSP: CycleSeriesPredicate): TimePredicate = {
    def series(t: TimeObject, context: TimeContext) = {
      val baseTime = context.refTime
      // 确定起点
      val (past, future) = runPredicate(cycleSP)(baseTime, context)
      val fut = future match {
        case ahead #:: rest if notImmediate && timeIntersect(ahead)(baseTime).nonEmpty => rest
        case _                                                                         => future
      }
      val n = if (context.reverseTake) -literalN else literalN
      val slot: Option[TimeObject] = if (n >= 0) {
        fut match {
          case start #:: _ =>
            val end = timePlus(start, cycleSP.grain, n)
            timeInterval(Open, start, end)
          case _ => None
        }
      } else {
        past match {
          case end #:: _ =>
            val start = timePlus(end, cycleSP.grain, n + 1)
            timeInterval(Closed, start, end)
          case _ => None
        }
      }
      slot match {
        case Some(nth) =>
          if (timeStartsBeforeTheEndOf(t)(nth)) (Stream.empty, Stream(nth))
          else (Stream(nth), Stream.empty)
        case _ => (Stream.empty, Stream.empty)
      }
    }

    SeriesPredicate(series, cycleSP.maxGrain)
  }

  /**
    * -1 is the first element in the past
    * 0 is the first element in the future
    */
  def takeNth(n: Int, notImmediate: Boolean, f: TimePredicate): TimePredicate = {
    val series = (t: TimeObject, context: TimeContext) => {
      val (past, future) = runPredicate(f)(context.refTime, context)
      val rest = if (n >= 0) {
        future match {
          case Stream.Empty => Stream.Empty
          case ahead #:: _ =>
            val series =
              if (notImmediate && timeIntersect(ahead)(context.refTime).nonEmpty) future.drop(n + 1)
              // 如果事件还未发生，那么下一个就是未来第一个未发生的，不需要drop
//              else if (timeBefore(context.refTime, ahead) && n >= 1) future.drop(n - 1)
              else future.drop(n)
            series
        }
      } else past.drop(-(n + 1))
      rest match {
        case Stream.Empty => (Stream.empty, Stream.empty)
        case nth #:: _ =>
          if (timeStartsBeforeTheEndOf(t)(nth)) (Stream.empty, Stream(nth))
          else (Stream(nth), Stream.empty)
      }
    }
    SeriesPredicate(series, f.maxGrain)
  }

  /**
    * Assumes the grain of `pred1` is smaller than the one of `pred2`
    */
  def timeCompose(pred1: TimePredicate, pred2: TimePredicate): TimePredicate = {
    mkIntersectPredicate(pred1, pred2)
  }

  def solarTermPredicate(term: String): SeriesPredicate = {
    val series: SeriesPredicateF = (t: TimeObject, context: TimeContext) => {
      if (!containSolarTerm(t.start.year, term)) (Stream.empty, Stream.empty)
      else {
        def f(step: Int)(to: TimeObject): TimeObject = {
          val y = to.start.year
          val dt = new DuckDateTime(solarTermTable.get(y + step, term).atStartOfDay().atZone(ZoneCN))
          TimeObject(dt, Day)
        }

        // 粒度调整，避免在节日当天比较出错
        val _t = timeRound(t, Day)

        val termOfThisYear = new DuckDateTime(solarTermTable.get(_t.start.year, term).atStartOfDay().atZone(ZoneCN))
        val date =
          if (termOfThisYear.isBefore(_t.start)) {
            new DuckDateTime(solarTermTable.get(t.start.year + 1, term).atStartOfDay().atZone(ZoneCN))
          } else {
            termOfThisYear
          }
        val anchor = TimeObject(date, Day)
        (Stream.iterate(anchor)(f(-1)).tail, Stream.iterate(anchor)(f(1)))
      }
    }
    SeriesPredicate(series, Some(Day))
  }

  /**
    * 判断是否包含year年的term节气
    * 当前和前后一年都必须存在，防止边界问题导致的空指针异常
    * @param year 年份
    * @param term 节气
    * @return     true/false
    */
  private def containSolarTerm(year: Int, term: String): Boolean = {
    if (!solarTermTable.contains(year, term) || !solarTermTable.contains(year -1, term) ||
        !solarTermTable.contains(year + 1, term)) false
    else true
  }
}
