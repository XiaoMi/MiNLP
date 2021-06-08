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

import scalaz.std.string.{parseDouble, parseLong}

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.{regexMatch, singleRegexMatch}
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.numeral.Prods._
import com.xiaomi.duckling.dimension.numeral.seq.{DigitSequence, token => _, _}

trait Rules extends DimRules {

  val dot = "."
  val comma = ","
  val zero = "0"

  val ruleIntegerNumeric = Rule(
    name = "integer (numeric)",
    pattern = List("(0|[1-9]\\d{0,17})".regex),
    prod = singleRegexMatch { case t =>
      parseLong(t).toOption.flatMap(long).map{ case Token(dim, nd: NumeralData) =>
        Token(dim, nd.copy(sequence = t))
      }
    }
  )

  val ruleInteger = Rule(
    name = "integer (0..10)",
    pattern = List("(〇|零|幺|一|二|两|兩|俩|三|仨|四|五|六|七|八|九|十|壹|贰|叁|肆|伍|陆|柒|捌|玖|拾)".regex),
    prod = {
      case (options: Options, Token(RegexMatch, GroupMatch(m :: _)) :: _) if integerMap.contains(m) =>
        val v = integerMap(m)
        if (m == "俩" || m == "仨") {
          if (!options.numeralOptions.dialectSupport) None
          else token(NumeralData(v, composable = false))
        } else integerMap.get(m).flatMap(i => long(i))
    }
  )
  val ruleNumeralsPrefixWithNegativeOrMinus = Rule(
    name = "numbers prefix with -, negative or minus",
    pattern = List("(-|负)".regex, isPositive.predicate),
    prod = tokens {
      case _ :: Token(Numeral, nd: NumeralData) :: _ => double(-nd.value, nd.precision)
    }
  )

  val ruleIntegerWithThousandsSeparator = Rule(
    name = "integer with thousands separator ,",
    pattern = List(raw"(\d{1,3}(,\d\d\d){1,5})".regex),
    prod = singleRegexMatch {
      case text => parseDouble(text.replace(comma, "")).toOption.flatMap(x => double(x))
    }
  )

  val ruleDecimalNumeral =
    Rule(name = "digit <fraction>", pattern = List("\\.(\\d+)".regex), prod = regexMatch {
      case m :: digits :: _ => parseDecimal(m, digits.length)
    })

  val ruleDecimalCharNumeral =
    Rule(
      name = "<integral>.<fractional>",
      pattern = List(
        // 12.5 的整数部分 DigitSequence/Numeral分不开，强制区分
        or(isComposable, or(isDigitLengthGt(10), isZhDigit)).predicate,
        "(点|\\.)".regex,
        or(isIntegerBetween(0, 9), isDimension(DigitSequence)).predicate
      ),
      prod = tokens {
        case Token(_, data1) :: _ :: Token(_, data2) :: _ =>
          val intPart = data1 match {
            case NumeralData(v, _, _, _, _, _) => v
            case DigitSequenceData(s, _, _) => s.toDouble
          }
          val fractionPart = data2 match {
            case NumeralData(v, _, _, _, _, _) =>
              for (vv <- getIntValue(v)) yield vv / math.pow(10, vv.toString.length)
            case DigitSequenceData(s, _, _) =>
              Some(s.toDouble / math.pow(10, s.length))
          }

          val precision = data2 match { // 小数精度
            case DigitSequenceData(s, _, _) => s.length
            case _ => 1
          }

          fractionPart.map { p =>
            val v = intPart + p
            token(NumeralData(v, composable = false, precision = precision))
          }
      }
    )

  val ruleNumeralsIntersectNonconsectiveUnit = Rule(
    name = "integer with nonconsecutive unit modifiers",
    pattern = List(isPositive.predicate, "(零|〇)".regex, isPositive.predicate),
    prod = tokens {
      case Token(Numeral, nd1: NumeralData) :: _ :: Token(Numeral, nd2: NumeralData) :: _
        if nd1.value > nd2.value =>
        def sumConnectedNumbers(d: Int): Option[Double] = {
          if (d == 0) None
          else Some(nd1.value + nd2.value)
        }

        if (nd1.value.toInt % 10 == 0) {
          for (n <- sumConnectedNumbers(diffIntegerDigits(nd1.value, nd2.value))) yield {
            Token(Numeral, NumeralData(value = n))
          }
        } else None
    }
  )
  val ruleRomanNumeric = Rule(
    name = "Roman (numeric)",
    pattern = List("(Ⅰ|Ⅱ|Ⅲ|Ⅳ|Ⅴ|Ⅵ|Ⅶ|Ⅷ|Ⅸ)".regex),
    prod = tokens {
      case Token(RegexMatch, GroupMatch(m :: _)) :: _ =>
        romanToIntergerMap.get(m).flatMap(long)
    }
  )
  val ruleNumeralsIntersectConsecutiveUnit = Rule(
    name = "integer with consecutive unit modifiers",
    pattern = List(
      and(isPositive, isComposable).predicate,
      and(isInteger, isPositive, not(isMultipliable), not(isCnSequence)).predicate
    ),
    prod = tokens {
      case Token(_, n1: NumeralData) :: Token(_, n2: NumeralData)
        :: _ =>
        def sumConnectedNumbers(d: Int): Option[Double] = {
          if (d == 0) Some(n1.value + n2.value)
          else None
        }

        if (n1.value.toInt % 10 == 0) {
          for (n <- sumConnectedNumbers(diffIntegerDigits(n1.value, n2.value))) yield {
            Token(Numeral, NumeralData(value = n, multipliable = true, composable = n2.composable))
          }
        } else None
    }
  )

  val ruleMultiplys = Rule(
    name = "compose by multiplication of [十百千万亿]",
    pattern =
      List(and(isNumeralDimension, not(isCnSequence)).predicate, numeralSuffixListPattern.regex),
    prod = tokens {
      case (t1@Token(_, NumeralData(value, _, _, _, _, _))) :: Token(_, GroupMatch(unit :: _)) :: _ =>
        // 不支持 3.5千/百/十
        if (!isInteger(value) && unit.matches("[十百千]")) None
        // 不支持30百，30千
        else if (isInteger(value) && value > 9.9 && unit.matches("[十百千]")) None
        else {
          for (nd <- multiply(t1, numeralSuffixList(unit.toLowerCase()))) yield {
            token(nd)
          }
        }
    }
  )

  val ruleMultiplysWithSingle = Rule(
    name = "compose like [七百三]",
    pattern = List(
      and(isNumeralDimension, not(isCnSequence)).predicate,
      // 去掉[三十五]这种情况
      numeralSuffixWithoutTenPattern.regex,
      s"(?i)(一|二|三|四|五|六|七|八|九)$numeralSuffixNegativePattern".regex
    ),
    prod = tokens {
      case t1 :: Token(_, GroupMatch(unit :: _)) :: Token(_, GroupMatch(c :: _)) :: _ =>
        for (v <- multiply(t1, numeralSuffixList(unit.toLowerCase()), integerMap(c))) yield {
          Token(Numeral, NumeralData(v, composable = false))
        }
    }
  )

  val ruleCnSequence = Rule(
    name = "cn sequence like 幺幺四",
    pattern = List(and(isZhDigit, isDigitLengthLt(50)).predicate),
    prod = opt {
      case (options: NumeralOptions, Token(DigitSequence, DigitSequenceData(seq, _, raw)) :: _)
        if options.cnSequenceAsNumber && !raw.contains("两二") && !raw.contains("二两") =>
        Token(Numeral, NumeralData(value = seq.toDouble, sequence = raw))
    }
  )

  val rule0DigitSequence = Rule(
    name = "sequence like 00011",
    pattern = List(and(isDigitLeading0, isDigitLengthLt(50)).predicate),
    prod = opt {
      case (options: NumeralOptions, Token(DigitSequence, DigitSequenceData(seq, _, raw)) :: _)
        if options.allowZeroLeadingDigits =>
        Token(Numeral, NumeralData(value = seq.toDouble, sequence = raw))
    }
  )
}
