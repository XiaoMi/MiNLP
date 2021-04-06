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

package duckling.dimension.time.date

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.matcher.GroupMatch
import duckling.dimension.matcher.Prods._
import duckling.dimension.numeral.Predicates._
import duckling.dimension.time._
import duckling.dimension.time.predicates._
import duckling.dimension.time.enums.{Hint, Lunar}
import duckling.dimension.time.helper.TimeDataHelpers._

object LunarDates {

  /**
    * 农历八月十五，1988年农历八月十五，农历1988年八月十五
    */
  val ruleLunarSymbolDate = Rule(
    name = "<lunar> <day>",
    pattern = List(
      "(农|阴)历".regex,
      // 避免农历八月初八按[农历]八月初八和[农历八月]初八结合两次
      and(isTimeDatePredicate, isNotHint(Hint.Intersect), isNotHint(Hint.YearMonth)).predicate
    ),
    prod = {
      case _ :: Token(Date, td: TimeData) :: _ =>
        val t = lunar(td).copy(calendar = Lunar(false), hint = Hint.Lunar)
        Token(Date, t)
    }
  )

  val ruleLunarMonth = Rule(
    name = "<lunar-month> 正/腊/冬",
    pattern = List("闰?(正|腊|冬)月".regex),
    prod = singleRegexMatch {
      case s =>
        val isLeap = s(0) == '闰'
        val m = (if (isLeap) s(1) else s(0)) match {
          case '正' => 1
          case '腊' => 12
          case '冬' => 11
        }
        val td = lunar(month(m), isLeap).copy(calendar = Lunar(isLeapMonth = isLeap))
        Token(Date, td)
    }
  )

  val ruleLunarDayOfMonth = Rule(
    name = "<lunar:day of month> 初/廿/卅",
    pattern = List("(初|廿|卅)".regex, isIntegerBetween(1, 10).predicate),
    prod = {
      case Token(_, GroupMatch(s :: _)) :: t2 :: _ =>
        def nday(n: Int): Option[Int] = {
          s match {
            case "初" => 0 + n
            case "廿" => if (n == 10) 20 else 20 + n
            case "卅" => if (n == 10) 30 else None
          }
        }

        for {
          n <- getIntValue(t2)
          day <- nday(n.toInt)
        } yield {
          Token(Date, lunar(dayOfMonth(day).copy(hint = Hint.DayOnly, calendar = Lunar(false))))
        }
    }
  )

  val rules = List(ruleLunarSymbolDate, ruleLunarMonth, ruleLunarDayOfMonth)
}
