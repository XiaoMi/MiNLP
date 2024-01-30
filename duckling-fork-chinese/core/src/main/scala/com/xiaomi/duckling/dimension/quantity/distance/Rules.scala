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

package com.xiaomi.duckling.dimension.quantity.distance

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.quantity.QuantityData

trait Rules extends DimRules {

  val dim = "Distance"

  private val scalar: Map[String, Double] = Map(
    "千米" -> 1000,
    "公里" -> 1000,
    "km" -> 1000,
    "米" -> 1,
    "m" -> 1,
    "里地" -> 500,
    "里" -> 500,
    "英里" -> 1609.31,
    "海里" -> 1853,
    "分米" -> 0.1,
    "dm" -> 0.1,
    "厘米" -> 0.01,
    "cm" -> 0.01,
    "毫米" -> 0.001,
    "mm" -> 0.001,
    "微米" -> 1.0E-6,
    "µm" -> 1.0E-6,
    "纳米" -> 1.0E-9,
    "nm" -> 1.0E-9,
    "皮米" -> 1.0E-12,
    "pm" -> 1.0E-12,
    "丈" -> 3.33,
    "尺" -> 0.333,
    "英尺" -> 0.304794,
    "寸" -> 0.0333,
    "英寸" -> 0.025399,
    "码" -> 0.914383,
    "光年" -> 9.46E+15
  )
  
  def unit(s: String): String = s match {
    case "千米" | "km" => "千米"
    case "米" | "m" => "米"
    case "里地" | "里" => "里"
    case "英里" | "英里" => "英里"
    case "海里" | "海里" => "海里"
    case "分米" | "dm" => "分米"
    case "厘米" | "cm" => "厘米"
    case "毫米" | "mm" => "毫米"
    case "微米" | "µm" => "微米"
    case "纳米" | "nm" => "纳米"
    case "皮米" | "pm" => "皮米"
    case _ => s
  }

  val numberKm = Rule(
    name = "<number> km",
    pattern = List(isDimension(Numeral).predicate, "(?i)千米|公里|km|里地|英里|海里|分米|dm|厘米|cm|毫米|mm|微米|µm|纳米|nm|皮米|pm|英尺|英寸|光年|丈|码|寸|尺|米|m|里".regex),
    prod = tokens {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        scalar.get(s.toLowerCase) match {
          case Some(t) => Token(Distance, QuantityData(value * t, "米", dim, originValue = Some(value), originUnit = Some(unit(s.toLowerCase))))
          case None => throw new IllegalArgumentException(s"unknown scalar found: $s")
        }
    }
  )
}
