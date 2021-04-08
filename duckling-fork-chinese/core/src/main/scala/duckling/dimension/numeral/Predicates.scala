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

package duckling.dimension.numeral

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.ordinal.{Ordinal, OrdinalData}
import duckling.dimension.time.unitNumber.UnitNumber

object Predicates {
  val isPositive: Predicate = {
    case Token(Numeral, NumeralData(v, _, _, _, _, _)) => v > 0.0
  }

  val is10s: Predicate = {
    case Token(Numeral, NumeralData(v, _, _, _, _, _)) =>
      getIntValue(v).exists(n => n % 10 == 0 && n / 100 == 0)
  }

  val isDigits: Predicate = {
    case Token(Numeral, NumeralData(v, _, _, _, _, _)) =>
      getIntValue(v).exists(n => n > 0 && n < 10)
  }

  val isNumeralDimension: Predicate = {
    case token => isDimension(Numeral)(token)
  }

  val isMultipliable: Predicate = {
    case Token(Numeral, NumeralData(_, _, multipliable, _, _, _)) => multipliable
  }

  val isIntegerBetween: (Int, Int) => Predicate = (low: Int, high: Int) => {
    case Token(Numeral, NumeralData(v, _, _, _, _, _)) => isIntegerBetween(v, low, high)
  }

  def isIntegerBetween(x: Double, low: Int, high: Int): Boolean = {
    low <= x && x <= high
  }

  val isCnSequence: Predicate = {
    case Token(Numeral, nd: NumeralData) => nd.isCnSeq
  }

  val isNatural: Predicate = {
    case Token(Numeral, NumeralData(v, _, _, _, _, _)) => isInteger(v) && v > 0
    case Token(UnitNumber, NumeralData(v, _, _, _, _, _)) => isInteger(v) && v > 0
    case _ => false
  }

  val isInteger: Predicate = {
    case Token(_, NumeralData(v, _, _, _, _, _)) => isInteger(v)
  }

  def isInteger(d: Double): Boolean = getIntValue(d).nonEmpty

  def getIntValue(token: Token): Option[Long] = token match {
    case Token(_, NumeralData(v, _, _, _, _, _)) => getIntValue(v)
    case Token(Ordinal, OrdinalData(v, _)) => v
    case _ => None
  }

  def getIntValue(x: Double): Option[Long] = {
    val int = x.toLong
    val rest = x - int
    if (rest == 0) Some(int) else None
  }

  val isComposable: Predicate = {
    case Token(Numeral, nd: NumeralData) => nd.composable
  }
}
