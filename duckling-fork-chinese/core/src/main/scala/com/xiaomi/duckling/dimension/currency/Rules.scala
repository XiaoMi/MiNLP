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
import com.xiaomi.duckling.dimension.numeral.NumeralData
import com.xiaomi.duckling.dimension.numeral.Predicates._

trait Rules extends DimRules {

  val RMB = "CNY"

  val numberUnit = Rule(
    name = "<number> <RMB unit>",
    pattern = List(isNatural.predicate, "(元|块|角|毛|分)钱?".regex),
    prod = tokens {
      case t1 :: Token(_, GroupMatch(s0 :: s :: _)) :: _ =>
        for (v <- getIntValue(t1)) yield {
          val scalar = s match {
            case "元" | "块" => 1.0
            case "角" | "毛" => 0.1
            case "分" => 0.01
          }
          token(CurrencyData(v, scalar, s, end = s0.endsWith("钱"), code = RMB))
        }
    }
  )

  def compose(cd1: CurrencyData, cd2: CurrencyData): Option[Token] = {
    if (cd1.scalar / cd2.scalar > 9.9) {
      if (cd1.code.nonEmpty && cd2.code.nonEmpty && cd1.code != cd2.code) None
      else {
        val code = if (cd1.code.nonEmpty) cd1.code else cd2.code
        Some(token(CurrencyData(cd1.v * cd1.scalar / cd2.scalar + cd2.v, cd2.scalar, "", compose = true, code = code)))
      }
    } else None
  }

  val compose1 = Rule(
    name = "<currency u1> <currency u2>",
    // 限定组合嵌套只能出现在左侧
    pattern = List(and(isNotAbbr, isNotEnd).predicate, isNotComposed.predicate),
    prod = tokens {
      case Token(_, cd1: CurrencyData) :: Token(_, cd2: CurrencyData) :: _ =>
        compose(cd1, cd2)
    }
  )

  val compose2 = Rule(
    name = "<currency u1> 零 <currency u2>",
    // 限定组合嵌套只能出现在左侧
    pattern = List(and(isNotAbbr, isNotEnd).predicate, "零".regex, isNotComposed.predicate),
    prod = tokens {
      case Token(_, cd1: CurrencyData) :: _ :: Token(_, cd2: CurrencyData) :: _ =>
        compose(cd1, cd2)
    }
  )

  /**
    * 九毛九，九块九
    */
  val abbr = Rule(
    name = "<currency u1> <number>",
    pattern = List(and(isNotFen, isNotEnd).predicate, isIntegerBetween(1, 9).predicate),
    prod = tokens {
      case Token(_, CurrencyData(v, scalar, unitText, _, _, _, _)) :: t2 :: _
        if unitText == "块" || unitText == "毛" =>
        for (i <- getIntValue(t2)) yield {
          token(CurrencyData(v * 10 + i, scalar * 0.1, "", abbr = true, code = RMB))
        }
    }
  )

  val rmb1 = Rule(
    name = "RMB <number>",
    pattern = List("(?i)rmb".regex, or(isNumeralDimension, isDimension(Currency)).predicate),
    prod = tokens {
      case _ :: Token(_, c @ CurrencyData(_, _, _, _, _, _, code)) :: _ =>
        if (code.nonEmpty && code.get != RMB) None
        else token(c.copy(code = RMB))
      case _ :: Token(_, NumeralData(value, _, _, _, _, _)) :: _ =>
        token(CurrencyData(value, 1, "", code = RMB))
    }
  )

  val rmb2 = Rule(
    name = "<number> RMB",
    pattern = List(or(isNumeralDimension, isDimension(Currency)).predicate, "(?i)rmb".regex),
    prod = tokens {
      case Token(_, c @ CurrencyData(_, _, _, _, _, _, code)) :: _ =>
        if (code.nonEmpty && code.get != RMB) None
        else token(c.copy(code = RMB))
      case Token(_, NumeralData(value, _, _, _, _, _)) :: _ :: _ =>
        token(CurrencyData(value, 1, "", code = RMB))
    }
  )
}
