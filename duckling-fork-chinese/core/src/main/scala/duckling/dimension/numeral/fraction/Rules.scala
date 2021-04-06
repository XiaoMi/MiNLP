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

package duckling.dimension.numeral.fraction

import duckling.Types._
import duckling.dimension.matcher.GroupMatch
import duckling.dimension.numeral.{Numeral, NumeralData}
import duckling.dimension.numeral.Predicates._
import duckling.dimension.DimRules
import duckling.dimension.implicits._

trait Rules extends DimRules {
  val fenZhi = Rule(
    name = "<number> 分之 <number>",
    pattern = List(isDimension(Numeral).predicate, "分之".regex, isPositive.predicate),
    prod = {
      case Token(_, n1: NumeralData) :: _ :: Token(_, n2: NumeralData) :: _ =>
        val v = if (n1.value == 0) 0 else n2.value / n1.value
        fraction(v, n2.value, n1.value)
    }
  )

  val slashSymbol = Rule(
    name = "<number> / <number>",
    pattern = List(isDimension(Numeral).predicate, "/".regex, isDimension(Numeral).predicate),
    prod = {
      case Token(_, n1: NumeralData) :: _ :: Token(_, n2: NumeralData) :: _ =>
        val v = if (n2.value == 0) 0 else n1.value / n2.value
        fraction(v, n1.value, n2.value)
    }
  )

  val percentLike = Rule(
    name = "|thousand|th of <number>",
    pattern = List("(百|千|万)分之".regex, isDimension(Numeral).predicate),
    prod = {
      case Token(_, GroupMatch(_ :: s :: _)) :: Token(_, n: NumeralData) :: _ =>
        val scalar = s match {
          case "百" => 100.0
          case "千" => 1000.0
          case "万" => 10000.0
        }
        fraction(n.value / scalar, n.value, scalar)
    }
  )

  val percentSymbol = Rule(
    name = "<number> percent symbol",
    pattern = List(isDimension(Numeral).predicate, "(%|‰)".regex),
    prod = {
      case Token(_, n: NumeralData) :: Token(_, GroupMatch(s :: _)) :: _ =>
        val scalar = s match {
          case "%" => 0.01
          case "‰" => 0.001
        }
        fraction(n.value * scalar, n.value, 1/scalar)
    }
  )

  val oneHalf =
    Rule(name = "<1> half", pattern = List(isIntegerBetween(1, 1).predicate, "半".regex), prod = {
      case _ => fraction(0.5, 50.0, 100.0)
    })

  val hundredPercentRule = Rule(
    name = "100%",
    pattern = List("百分之?百".regex),
    prod = {
      case _ => fraction(1.0, 100.0, 100.0)
    }
  )
}
