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

import com.google.common.collect.Table
import java.time.LocalDate

import com.xiaomi.duckling.Types.{conf, _}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, LexiconMatch, LexiconMatches}
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.TimeData
import com.xiaomi.duckling.dimension.time.date.Date
import com.xiaomi.duckling.dimension.time.enums.Grain.Day
import com.xiaomi.duckling.dimension.time.enums.IntervalType.Open
import com.xiaomi.duckling.dimension.time.helper.SolarTermProvider
import com.xiaomi.duckling.dimension.time.helper.TimePredicateHelpers._
import com.xiaomi.duckling.engine.LexiconLookup.Dict
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.enums.Hint
import com.xiaomi.duckling.dimension.time.predicates.SequencePredicate

object SolarTerms {

  private val provdier = Class
    .forName(conf.getString("dimension.time.solar.provider"))
    .getDeclaredConstructor()
    .newInstance()
    .asInstanceOf[SolarTermProvider]

  def dict: Dict = provdier.dict

  def solarTermTable: Table[Int, String, LocalDate] = provdier.solarTermTable

  val solarTermRule = Rule(
    name = "<solar term>",
    pattern = List(dict.lexicon),
    prod = tokens {
      case Token(LexiconMatch, LexiconMatches(s, t)) :: _ =>
        val td = TimeData(
          timePred = solarTermPredicate(t),
          timeGrain = Day,
          okForThisNext = true,
          holiday = t
        )
        tt(td)
    }
  )
  
  val seasonsRule = Rule(
    name = "<four seasons>",
    pattern = List("(春|夏|秋|冬)(天|季)".regex),
    prod = optTokens {
      case (options: Options, Token(_, GroupMatch(_ :: s :: _)) :: _) =>
        if (options.timeOptions.parseFourSeasons) {
          val (holiday, ss, se) = s match {
            case "春" => ("春季", "立春", "立夏")
            case "夏" => ("夏季", "立夏", "立秋")
            case "秋" => ("秋季", "立秋", "立冬")
            case _    => ("冬季", "立冬", "立春")
          }
  
          val from = TimeData(timePred = solarTermPredicate(ss), timeGrain = Day, okForThisNext = true, holiday = ss, hint = Hint.Season)
          val to = TimeData(timePred = solarTermPredicate(se), timeGrain = Day, okForThisNext = true, holiday = se, hint = Hint.Season)
  
          for (td <- interval(Open, from, to, options.timeOptions.beforeEndOfInterval)) yield Token(Date, td.copy(holiday = holiday, hint = Hint.Season))
        } else None
    })

  val rules = List(solarTermRule, seasonsRule)
}
