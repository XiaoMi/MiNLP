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

package com.xiaomi.duckling.dimension.quantity.velocity

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.quantity.QuantityData

trait Rules extends DimRules {

  val dim = "Velocity"

  def unit(s: String): String = s match {
    case "英里" | "迈" | "码" => "英里每小时"
    case "千米" | "公里" => "千米每小时"
    case "米" => "米每秒"
    case "英尺" | "尺" => "英尺每秒"
  }

  val numberKmPerHour = Rule(
    name = "<number> km/h",
    pattern = List(isDimension(Numeral).predicate, "((千米|公里|英里|迈|码)(每|一)小时|码|迈)".regex),
    prod = {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(groups)) :: _ =>
        val u = if (groups(2) != "") unit(groups(2)) else unit(groups(1))
        Token(Velocity, QuantityData(value, u, dim))
    }
  )

  val perHourNumberKm = Rule(
    name = "per <number> km",
    pattern = List("(每|一)小时".regex, isDimension(Numeral).predicate, "(千米|公里|英里|迈|码)".regex),
    prod = {
      case _ :: Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        Token(Velocity, QuantityData(value, unit(s), "Velocity"))
    }
  )

  val numberMPerSecond = Rule(
    name = "<number> m/s",
    pattern = List(isDimension(Numeral).predicate, "(米|英尺|尺)[每一]秒".regex),
    prod = {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(_ :: s :: _)) :: _ =>
        Token(Velocity, QuantityData(value, unit(s), dim))
    }
  )

  val perSecondNumberM = Rule(
    name = "per <number> km",
    pattern = List("(每|一)秒".regex, isDimension(Numeral).predicate, "(米|英尺|尺)".regex),
    prod = {
      case _ :: Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        Token(Velocity, QuantityData(value, unit(s), "Velocity"))
    }
  )
}
