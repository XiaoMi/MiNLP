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

import java.time.LocalDate

import com.google.common.collect.Table

import duckling.dimension.time.enums.Grain.Day
import duckling.dimension.time.helper.TimePredicateHelpers._
import duckling.dimension.time.Prods._
import duckling.Types.{conf, _}
import duckling.dimension.implicits._
import duckling.dimension.matcher.{LexiconMatch, LexiconMatches}
import duckling.dimension.time.helper.SolarTermProvider
import duckling.dimension.time.TimeData
import duckling.engine.LexiconLookup.Dict

object SolarTerms {

  private val provdier = Class
    .forName(conf.getString("dimension.time.solar.provider"))
    .newInstance()
    .asInstanceOf[SolarTermProvider]

  def dict: Dict = provdier.dict

  def solarTermTable: Table[Int, String, LocalDate] = provdier.solarTermTable

  val rule = Rule(
    name = "<solar term>",
    pattern = List(dict.lexicon),
    prod = {
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

  val rules = List(rule)
}
