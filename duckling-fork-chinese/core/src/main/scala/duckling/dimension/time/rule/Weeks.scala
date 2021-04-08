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

package duckling.dimension.time.rule

import scalaz.Scalaz._

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.matcher.Prods.regexMatch
import duckling.dimension.time.{Time, TimeData}
import duckling.dimension.time.Helpers._
import duckling.dimension.time.helper.TimeDataHelpers._
import duckling.dimension.time.predicates._
import duckling.dimension.time.enums.Grain.Week
import duckling.dimension.time.Prods._

object Weeks {
  val ruleThisDayOfWeek = Rule(
    name = "this <day-of-week>",
    pattern = List("(这|今|本)个?".regex, isADayOfWeek.predicate),
    prod = {
      case _ :: Token(Time, td: TimeData) :: _ =>
        (predNth(0, false) >>> reset(Week) >>> tt) (td)
    }
  )

  val ruleRecentWeekday = Rule(
    name = "recent <day-of-week>",
    pattern = List("(这|今|本|下+|上+)(一?个)?(周|星期|礼拜)([1-7]|一|二|三|四|五|六|七|天(?!(气|津|长|水|门))|日)".regex),
    prod = regexMatch { case _ :: w :: _ :: _ :: day :: _ =>
      val n =
        if (w(0) == '下') w.length
        else if (w(0) == '上') -w.length
        else 0
      val d = day match {
        case "1" | "一" => 1
        case "2" | "二" => 2
        case "3" | "三" => 3
        case "4" | "四" => 4
        case "5" | "五" => 5
        case "6" | "六" => 6
        case "7" | "七" | "天" | "日" => 7
      }
      val weekday = dayOfWeek(d).copy(okForThisNext = true, notImmediate = false)
      val week = cycleNthThis(n, Week, Week)
      for(w <- week; td <- intersect(w, weekday)) yield tt(td)
    }
  )

  val ruleNextDayOfWeek = Rule(
    name = "next <day-of-week>",
    pattern = List("(明|下)个?".regex, isADayOfWeek.predicate),
    prod = {
      case _ :: Token(Time, td: TimeData) :: _ =>
        (predNth(1, false) >>> reset(Week) >>> tt) (td)
    }
  )

  val ruleWeekend = Rule(name = "week-end", pattern = List("周末".regex), prod = {
    case _ => tt(weekend)
  })

  def mkRuleDaysOfWeek(pairs: List[(String, String)]): List[Rule] = {
    pairs.zipWithIndex.map {
      case ((name, pattern), i) =>
        mkSingleRegexRule(
          name,
          pattern,
          tt(dayOfWeek(i + 1).copy(okForThisNext = true, notImmediate = false))
        )
    }
  }

  // TODO 测试简化为一条效率是否有提升
  val ruleDaysOfWeek = mkRuleDaysOfWeek(
    List(
      ("Monday", "(星期|周|礼拜)[一|1]"),
      ("Tuesday", "(星期|周|礼拜)[二|2]"),
      ("Wednesday", "(星期|周|礼拜)[三|3]"),
      ("Thursday", "(星期|周|礼拜)[四|4]"),
      ("Friday", "(星期|周|礼拜)[五|5]"),
      ("Saturday", "(星期|周|礼拜)[六|6]"),
      ("Sunday", "(星期|礼拜|周)(日|天(?!(气|津|长|水|门)))")
    )
  )

  val rules = List(ruleRecentWeekday, ruleWeekend) ++ ruleDaysOfWeek //ruleThisDayOfWeek, ruleNextDayOfWeek
}
