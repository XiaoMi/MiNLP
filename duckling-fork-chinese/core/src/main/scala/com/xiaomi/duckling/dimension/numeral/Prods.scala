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

package com.xiaomi.duckling.dimension.numeral

import scalaz.std.string.parseDouble

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._

object Prods {
  implicit class NumeralTokenWrapper(token: Token) {
    def withGrainMultipliable(g: Int): Option[Token] = withGrain(g).flatMap(_.withMultipliable())

    def withGrain(g: Int): Option[Token] = token match {
      case Token(Numeral, x: NumeralData) =>
        Some(Token(Numeral, x.copy(grain = Some(g))))
      case _ => None
    }

    def withMultipliable(): Option[Token] = token match {
      case Token(Numeral, x: NumeralData) =>
        Some(Token(Numeral, x.copy(multipliable = true)))
      case _ => None
    }
  }

  val numeralSuffixList: Map[String, Token] = Map(
    "k" -> double(1e3).flatMap(_.withGrainMultipliable(3)).get,
    "m" -> double(1e6).flatMap(_.withGrainMultipliable(6)).get,
    "g" -> double(1e9).flatMap(_.withGrainMultipliable(9)).get,
    "十" -> double(1e1).flatMap(_.withGrainMultipliable(1)).get,
    "百" -> double(1e2).flatMap(_.withGrainMultipliable(2)).get,
    "千" -> double(1e3).flatMap(_.withGrainMultipliable(3)).get,
    "万" -> double(1e4).flatMap(_.withGrainMultipliable(4)).get,
    "亿" -> double(1e8).flatMap(_.withGrainMultipliable(8)).get
  )

  val numeralSuffixListPattern = s"(?i)(${numeralSuffixList.keys.mkString("|")})"
  val numeralSuffixWithoutTenPattern =
    s"(?i)(${numeralSuffixList.keys.filterNot(_ == "十").mkString("|")})"
  val numeralSuffixNegativePattern = s"(?!(${numeralSuffixList.keys.mkString("|")}))"

  // rule integer
  val integerMap: Map[String, Int] = Map(
    "〇" -> 0,
    "零" -> 0,
    "一" -> 1,
    "幺" -> 1,
    "两" -> 2,
    "二" -> 2,
    "三" -> 3,
    "四" -> 4,
    "五" -> 5,
    "六" -> 6,
    "七" -> 7,
    "八" -> 8,
    "九" -> 9,
    "十" -> 10,
    "壹" -> 1,
    "贰" -> 2,
    "叁" -> 3,
    "肆" -> 4,
    "伍" -> 5,
    "陆" -> 6,
    "柒" -> 7,
    "捌" -> 8,
    "玖" -> 9,
    "拾" -> 10
  )
  val integerMap1: Map[String, Int] = Map(
    "〇" -> 0,
    "零" -> 0,
    "一" -> 1,
    "兩" -> 2,
    "两" -> 2,
    "二" -> 2,
    "三" -> 3,
    "四" -> 4,
    "五" -> 5,
    "六" -> 6,
    "七" -> 7,
    "八" -> 8,
    "九" -> 9,
    "十" -> 10,
    "1" -> 1,
    "2" -> 2,
    "3" -> 3,
    "4" -> 4,
    "5" -> 5
  )
  val romanToIntergerMap: Map[String, Long] =
    Map(
      "I" -> 1,
      "Ⅰ" -> 1,
      "Ⅱ" -> 2,
      "Ⅲ" -> 3,
      "Ⅳ" -> 4,
      "Ⅴ" -> 5,
      "Ⅵ" -> 6,
      "Ⅶ" -> 7,
      "Ⅷ" -> 8,
      "Ⅸ" -> 9
    )

  def long(x: Long): Option[Token] = double(x)

  def parseDecimal(`match`: String, precision: Int): Option[Token] = {
    parseDouble(`match`).toOption.flatMap(x => double(x, precision))
  }

  def token(nd: NumeralData) = Token(Numeral, nd)

  def double(x: Double, precision: Int = 0): Option[Token] = Some(Token(Numeral, NumeralData(value = x, precision=precision)))

  def divide(t1: Token, t2: Token): Option[Token] = {
    (t1, t2) match {
      case (
        Token(Numeral, NumeralData(v1, _, _, _, _, _)),
        Token(Numeral, NumeralData(v2, _, _, _, _, _))
        ) =>
        v1 / v2 match {
          case Double.NaN | Double.NegativeInfinity | Double.PositiveInfinity => None
          case v => double(v)
        }
    }
  }

  def multiply(t1: Token, t2: Token): Option[NumeralData] = {
    (t1, t2) match {
      case (
        Token(_, NumeralData(v1, _, _, _, _, _)),
        Token(Numeral, NumeralData(v2, grain, _, _, _, _))
        ) =>
        grain match {
          case None => NumeralData(v1 * v2)
          case Some(g) =>
            if (v2 > v1) NumeralData(v1 * v2, grain = g)
            else None
        }
    }
  }

  def multiply(t1: Token, t2: Token, n: Int): Option[Double] = {
    (t1, t2) match {
      case (
        Token(Numeral, NumeralData(v1, _, _, _, _, _)),
        Token(Numeral, NumeralData(v2, grain, _, _, _, _))
        ) =>
        grain match {
          case None => Some(v1 * v2)
          case Some(g) =>
            if (v2 > v1) Some(v1 * v2 + math.pow(10, g - 1) * n)
            else None
        }
    }
  }

  /**
    * diffIntegerDigits a b = # of digits in a - # of digits in b
    * ignores the nondecimal components
    *
    * @param a
    * @param b
    * @return
    */
  def diffIntegerDigits(a: Double, b: Double): Int = {
    def digitsOf(d: Double): Int = digitsOfInt(math.floor(math.abs(d)).toInt)

    def digitsOfInt(i: Int): Int = {
      if (i == 0) 0
      else 1 + digitsOfInt(i / 10)
    }

    def zerosOf(i: Int): Int = {
      if (i == 0) 0
      else if (i % 10 == 0) 1 + zerosOf(i / 10)
      else 0
    }

    val aInt = math.floor(math.abs(a)).toInt
    // 限定连接的时候末尾必须是0
    zerosOf(aInt) - digitsOf(b)
  }

  def uncomposable(nd: NumeralData): NumeralData = nd.copy(composable = false)
}
