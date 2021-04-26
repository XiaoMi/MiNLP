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

package com.xiaomi.duckling.dimension.currency

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.numeral.Predicates._

// TODO 九毛九，九块九，16.3元
trait Rules extends DimRules {

  val numberUnit = Rule(
    name = "<number> <RMB unit>",
    pattern = List(isNatural.predicate, "(元|块|角|毛|分)钱?".regex),
    prod = {
      case t1 :: Token(_, GroupMatch(s0 :: s :: _)) :: _ =>
        for (v <- getIntValue(t1)) yield {
          val scalar = s match {
            case "元" | "块" => 1
            case "角" | "毛" =>.1
            case "分" =>.01
          }
          token(CurrencyData(v, scalar, s, end = s0.endsWith("钱")))
        }
    }
  )

  val compose = Rule(
    name = "<currency u1> <currency u2>",
    // 限定组合嵌套只能出现在左侧
    pattern = List(and(isNotAbbr, isNotEnd).predicate, isNotComposed.predicate),
    prod = {
      case Token(_, cd1: CurrencyData) :: Token(_, cd2: CurrencyData) :: _
        if cd1.scalar / cd2.scalar > 9.9 =>
        token(CurrencyData(cd1.v * cd1.scalar / cd2.scalar + cd2.v, cd2.scalar, "", compose = true))
    }
  )

  /**
    * 九毛九，九块九
    */
  val abbr = Rule(
    name = "<currency u1> <number>",
    pattern = List(and(isNotFen, isNotEnd).predicate, isIntegerBetween(1, 9).predicate),
    prod = {
      case Token(_, CurrencyData(v, scalar, unitText, _, _, _)) :: t2 :: _
        if unitText == "块" || unitText == "毛" =>
        for (i <- getIntValue(t2)) yield {
          token(CurrencyData(v * 10 + i, scalar * 0.1, "", abbr = true))
        }
    }
  )
}
