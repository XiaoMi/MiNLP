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

package com.xiaomi.duckling.dimension.quantity.area

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.quantity.QuantityData

/**
 * 完整的面积包括很多，还需要进行单位换算，暂时只开个头
 */
trait Rules extends DimRules {

  val dim = "Area"

  val numberUnit = Rule(
    name = "<number> <area unit>",
    pattern = List(isDimension(Numeral).predicate, "(个?(平方米?|平米)|平)".regex),
    prod = tokens {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        Token(Area, QuantityData(value, "平方米", dim))
    }
  )

  val numberSKM = Rule(
    name = "<number> 平方公里",
    pattern = List(isDimension(Numeral).predicate, "平方(公里|千米)".regex),
    prod = tokens {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: _ =>
        Token(Area, QuantityData(value, "平方千米", dim))
    }
  )
}
