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

package com.xiaomi.duckling.dimension.act

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.ordinal.notEndsWithGe

trait Rules extends DimRules {
  private val actPattern = "(番|幕|弹|场)".regex

  val numeratedAct = Rule(
    name = "<ordinal> act",
    pattern = List(notEndsWithGe.predicate, actPattern),
    prod = tokens {
      case t1 :: _ =>
        for (i <- getIntValue(t1)) yield act(i.toInt)
    }
  )

  val integerAct = Rule(
    name = "<positive integer> act",
    pattern = List(isDigits.predicate, actPattern),
    prod = tokens {
      case t1 :: _ =>
        for (i <- getIntValue(t1)) yield act(i.toInt, true)
    }
  )

  val reverseAct = Rule(
    name = "reverse act",
    pattern = List(reversePattern, isNatural.predicate, actPattern),
    prod = tokens {
      case _ :: t1 :: _ =>
        for (i <- getIntValue(t1)) yield act(-i.toInt)
    }
  )
}
