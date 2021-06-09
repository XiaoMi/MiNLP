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

package com.xiaomi.duckling.dimension.level

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.ordinal.{OrdinalData, notEndsWithGe}

trait Rules extends DimRules {
  val ordinal = Rule(
    name = "<ordinal> level",
    pattern = List(notEndsWithGe.predicate, "(级(?!别)|档)".regex),
    prod = tokens {
      case Token(_, OrdinalData(value, _)) :: _ => Token(Level, NumeralData(value))
    }
  )

  val numeral = Rule(
    name = "<numeral> level",
    pattern = List(isDimension(Numeral).predicate, "(级(?!别)|档)".regex),
    prod = tokens {
      case t1 :: _ => t1.copy(dim = Level)
    }
  )
}
