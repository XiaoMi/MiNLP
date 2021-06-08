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

package com.xiaomi.duckling.dimension.episode

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.ordinal.notEndsWithGe
import com.xiaomi.duckling.dimension.quantity.QuantityData
import com.xiaomi.duckling.dimension.season.reversePattern

trait Rules extends DimRules {
  private val episodePattern = "(集|期|辑|回(?!答)|篇|章|课|讲)".regex

  val ordinalSeason = Rule(
    name = "<ordinal> episode",
    pattern = List(notEndsWithGe.predicate, episodePattern),
    prod = tokens {
      case t1 :: _ =>
        for (i <- getIntValue(t1)) yield Token(Episode, QuantityData(i, "集", "集"))
    }
  )

  val numberSeason = Rule(
    name = "<postive integer> episode",
    pattern = List(isNatural.predicate, episodePattern),
    prod = tokens {
      case t1 :: _ =>
        for (i <- getIntValue(t1)) yield Token(Episode, QuantityData(i, "集", "集", true))
    }
  )

  val reverseSeason = Rule(
    name = "reverse episode",
    pattern = List(reversePattern, isNatural.predicate, episodePattern),
    prod = tokens {
      case _ :: t1 :: _ =>
        for (i <- getIntValue(t1)) yield Token(Episode, QuantityData(-i, "集", "集"))
    }
  )
}
