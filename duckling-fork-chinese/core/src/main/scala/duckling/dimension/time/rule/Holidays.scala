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

import com.google.common.collect.ImmutableMap

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.matcher.{LexiconMatch, LexiconMatches}
import duckling.dimension.time.helper.HolidayProvider
import duckling.dimension.time.rule.LunarDays.ruleLunarNewYearsEve
import duckling.engine.LexiconLookup.Dict


object Holidays {
  private val provdier = Class
      .forName(conf.getString("dimension.time.holiday.provider"))
      .newInstance()
      .asInstanceOf[HolidayProvider]


  def dict: Dict = provdier.dict

  def holidayTokenMap: ImmutableMap[String, Token] = provdier.holidayTokenMap

  val rule = Rule(
    name = "<holidays>",
    pattern = List(dict.lexicon),
    prod = {
      case Token(LexiconMatch, LexiconMatches(s, t)) :: _ =>
				holidayTokenMap.get(t)
    }
  )

  val rules = List(rule, ruleLunarNewYearsEve)
}
