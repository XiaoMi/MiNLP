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

package com.xiaomi.duckling.dimension.time.rule

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods._
import com.xiaomi.duckling.dimension.time.{Time, TimeConstant, TimeData, intersectSchema}
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.Hint
import com.xiaomi.duckling.dimension.time.enums.Hint._
import com.xiaomi.duckling.dimension.time.enums.IntervalType.Open
import com.xiaomi.duckling.dimension.time.form.{IntervalOfDay, PartOfDay, TimeOfDay}
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.{SequencePredicate, TimeDatePredicate, TimeIntervalsPredicate, _}
import com.xiaomi.duckling.dimension.time.{Time, TimeData}

object FuzzyDayIntervals {

  val pattern = "(早上|早晨|晨间|下午|晚上|中午|午间|上午|傍晚|黄昏|凌晨|半夜|夜间|夜晚|夜里)"

  def between(s: String): (String, (Int, Int), String) = {
    s match {
      case "早上" | "早晨" | "晨间"               => ("早上", (4, 12), TimeConstant.EM)
      case "上午"                             => ("上午", (8, 12), TimeConstant.FN)
      case "中午" | "午间"                      => ("中午", (12, 14), TimeConstant.NO)
      case "下午"                             => ("下午", (12, 18), TimeConstant.AN)
      case "晚上" | "晚间" | "夜里" | "夜间" | "夜晚" => ("晚上", (18, 0), TimeConstant.NI)
      case "午夜" | "凌晨" | "半夜"               => ("凌晨", (0, 6), TimeConstant.MN)
      case "傍晚" | "黄昏"                      => ("傍晚", (17, 19), TimeConstant.EV)
    }
  }

  def ofInterval(s: String): Option[(String, TimeData, String)] = {
    val (name, (from, to), schema) = between(s)
    interval(Open, hour(is12H = false, from), hour(is12H = false, to)).map((name, _, schema))
  }

  val ruleFuzzyDayIntervals =
    Rule(name = "fuzzy day intervals", pattern = List(pattern.regex), prod = singleRegexMatch {
      case s =>
        for ((name, td, schema) <- ofInterval(s)) yield {
          tt(partOfDay(name, td).copy(schema = schema))
        }
    })

  val ruleRecent = Rule(
    name = "recent tonight/morning omit unit",
    pattern = List("(今|明|昨)(早上?|中午|晚上?)".regex),
    prod = regexMatch {
      case _ :: s0 :: s1 :: _ =>
        val offset = s0 match {
          case "今" => 0
          case "明" => 1
          case "昨" => -1
        }
        val part = s1(0) match {
          case '早' => "早上"
          case '中' => "中午"
          case '晚' => "晚上"
        }
        val tdInterval = ofInterval(part)
        for {
          (_, td, schema) <- tdInterval
          td1 <- intersect(cycleNth(Day, offset), td)
        } yield {
          tt(partOfDay(part, td1).copy(schema =schema))
        }
    }
  )

  private def fuzzyIntervalTimeOfDay(td0: TimeData, td: TimeData): Option[Token] = {
    val TimeData(pred, _, _, _, Some(form), _, _, _, _, _, _, _) = td
    val part = td0.form.get.asInstanceOf[PartOfDay].part
    val schema = intersectSchema(td0, td)
    (pred, form) match {
      case (p: TimeDatePredicate, TimeOfDay(Some(h), is12H)) =>
        for {
          hAdjustP <- updatePredicateByFuzzyInterval(part, p)
          (is12H, hAdjust) <- hAdjustP.hour
        } yield {
          val tdAdjust =
            timeOfDay(hAdjust, is12H = false, td).copy(timePred = hAdjustP, hint = Hint.NoHint)
          tt(tdAdjust.copy(schema = schema))
        }
      case (
          TimeIntervalsPredicate(t, p1: TimeDatePredicate, p2: TimeDatePredicate),
          IntervalOfDay
          ) =>
        for {
          from <- updatePredicateByFuzzyInterval(part, p1)
          to <- updatePredicateByFuzzyInterval(part, p2)
        } yield tt(td.copy(timePred = TimeIntervalsPredicate(t, from, to), schema = schema))
      case _ => None
    }
  }

  val ruleFuzzyIntervalTimeOfDay1 = Rule(
    name = "fuzzy interval <time-of-day> 1",
    pattern = List(
      // 避免[2017年三月2号早上][10点半] 与 [2017年三月2号][早上10点半] 同时出现，只保留后者
      and(isAPartOfDay, not(isHint(PartOfDayAtLast))).predicate,
      and(isNotLatent, or(isATimeOfDay, isIntervalOfDay)).predicate
    ),
    prod = tokens {
      case Token(Time, td0: TimeData) :: Token(Time, td: TimeData) :: _ =>
        fuzzyIntervalTimeOfDay(td0, td)
    }
  )

  val ruleFuzzyIntervalTimeOfDay2 = Rule(
    name = "fuzzy interval <time-of-day> 2",
    pattern = List(
      // 避免[2017年三月2号早上][10点半] 与 [2017年三月2号][早上10点半] 同时出现，只保留后者
      and(isAPartOfDay, not(isHint(PartOfDayAtLast))).predicate,
      "的".regex,
      and(isNotLatent, or(isATimeOfDay, isIntervalOfDay)).predicate
    ),
    prod = tokens {
      case Token(Time, td0: TimeData) :: _ :: Token(Time, td: TimeData) :: _ =>
        fuzzyIntervalTimeOfDay(td0, td)
    }
  )

  def updatePredicateByFuzzyInterval(s: String, p: TimeDatePredicate): Option[TimeDatePredicate] = {
    for ((is12H, h) <- p.hour) yield {
      val hAdjust =
        if (h == 12 && s == "凌晨") 0
        else if (h > 10 && (s == "中午" || s == "午间")) h // 中午11点
        else if (h == 12 && s == "凌晨") 0
        else if (h == 12 && s == "晚上" || s == "晚间") 24
        else {
          val (_, (from, _), _) = between(s)
          if (from >= 12 && h < 12) h + 12 else h
        }
      p.copy(hour = Some(false, hAdjust))
    }
  }

  val rule24OClockOfDay = Rule(
    name = "24 o'clock of day",
    pattern = List(isADayOfMonth.predicate, is24oClockOfDay.predicate),
    prod = tokens {
      case Token(Time, td1: TimeData) :: Token(Time, td2: TimeData) :: _ =>
        val pred = SequencePredicate(List(td1, td2))
        tt(TimeData(pred, timeGrain = Hour))
    }
  )

  val rules = List(
    ruleFuzzyDayIntervals,
    ruleRecent,
    ruleFuzzyIntervalTimeOfDay1,
    ruleFuzzyIntervalTimeOfDay2,
    rule24OClockOfDay
  )
}
