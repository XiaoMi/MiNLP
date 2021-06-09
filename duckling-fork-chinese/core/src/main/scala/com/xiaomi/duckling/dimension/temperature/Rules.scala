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

package com.xiaomi.duckling.dimension.temperature

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.quantity.QuantityData

trait Rules extends DimRules {

  val negatives = List("零下", "负的", "负")
  val units = List("华氏", "°F", "摄氏", "°C", "°")

  private val unitPattern: String = units.mkString("(", "|", ")?度?")

  val ruleTemperatureWithNegative = Rule(
    name = "negative <number> unit like [华氏零下30度, 零下华氏30度, 零下30华氏度, 华氏30度]",
    pattern = List(
      s"(华氏|摄氏)?${negatives.mkString("(", "|", ")?")}(华氏|摄氏)?".regex,
      isDimension(Numeral).predicate,
      unitPattern.regex
    ),
    prod = tokens {
      case Token(RegexMatch, GroupMatch(prefix :: _)) :: Token(
      Numeral,
      NumeralData(value, _, _, _, _, _)
      ) :: Token(RegexMatch, GroupMatch(unitStr :: _)) :: _ =>
        val negative =
          if (prefix.nonEmpty && (prefix.contains("零下") || prefix.contains("负的") || prefix
            .contains("负"))) -1
          else +1
        val unit =
          if ((prefix + unitStr).contains("华氏") || (prefix + unitStr).contains("°F")) "F" else "C"
        Token(Temperature, QuantityData(negative * value, unit, "温度"))
    }
  )

  val ruleTemperature = Rule(
    name = "<number> unit like [30华氏度]",
    pattern = List(isDimension(Numeral).predicate, unitPattern.regex),
    prod = tokens {
      case Token(Numeral, NumeralData(value, _, _, _, _, _)) :: Token(
      RegexMatch,
      GroupMatch(unitStr :: _)
      ) :: _ =>
        val unit = if (unitStr.contains("华氏") || unitStr.contains("°F")) "F" else "C"
        Token(Temperature, QuantityData(value, unit, "温度"))
    }
  )
}
