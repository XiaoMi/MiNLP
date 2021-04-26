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

package com.xiaomi.duckling.dimension.ordinal

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates.isNumeralDimension
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}

trait Rules extends DimRules {

  val ruleOrdinalDigits = Rule(
    name = "ordinal (digits)",
    pattern = List("第".regex, isNumeralDimension.predicate),
    prod = {
      case _ :: Token(Numeral, NumeralData(v, _, _, _, _, _)) :: _ => ordinal(math.floor(v).toLong)
    }
  )

  val ruleOrdinalDigitsGe =
    Rule(name = "ordinal (digits) 个", pattern = List(notEndsWithGe.predicate, "个".regex), prod = {
      case Token(Ordinal, OrdinalData(value, _)) :: _ =>
        ordinal(value, ge = true)
    })
}
