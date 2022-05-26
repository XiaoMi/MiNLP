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
import com.xiaomi.duckling.dimension.matcher.{LexiconMatch, LexiconMatches}
import com.xiaomi.duckling.dimension.time.helper.HolidayProvider
import com.xiaomi.duckling.dimension.time.rule.LunarDays.ruleLunarNewYearsEve
import com.xiaomi.duckling.engine.LexiconLookup.Dict
import scala.collection.mutable.Map


object Holidays {
  private val provdier = Class
      .forName(conf.getString("dimension.time.holiday.provider"))
      .getDeclaredConstructor()
      .newInstance()
      .asInstanceOf[HolidayProvider]


  def dict: Dict = provdier.dict

  def holidayTokenMap: Map[String, Token] = provdier.holidayTokenMap

  val rule = Rule(
    name = "<holidays>",
    pattern = List(dict.lexicon),
    prod = tokens {
      case Token(LexiconMatch, LexiconMatches(s, t)) :: _ =>
				holidayTokenMap.get(t)
    }
  )

  val rules = List(rule, ruleLunarNewYearsEve)
}
