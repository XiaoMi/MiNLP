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

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types.RankOptions
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.numeral.fraction.{Fraction, FractionData}
import com.xiaomi.duckling.dimension.ordinal.{Ordinal, OrdinalData}
import com.xiaomi.duckling.ranking.Testing._
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSpec, Matchers}

class NumeralTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

  val options = testOptions.copy(
    targets = Set(Numeral),
    full = false,
    rankOptions = RankOptions(combinationRank = true)
  )

  describe("NumeralTest") {
    val cases = Table(
      ("sentence", "target"),
      ("十八二", "18/2"),
      ("一百零八七", "108/7"),
      ("三十四十", "30/40"),
      ("十三十四", "13/14"),
      ("三十四百", "30/400"),
      ("三十三百四十", "30/340"),
      ("五百六五百六", "560/560"),
      ("二百五三", "250/3")
    )

    it("numeral combination rank") {
      forAll(cases) {
        case (sentence, target) =>
          val answers = analyze(sentence, testContext, options)
          answers.flatMap(a => getIntValue(a.token.node.token)).mkString("/") shouldBe target
      }
    }

    describe("NumeralSchemaTest") {
      val testCases = List(
        ("零", Some("0.0")),
        ("十一", Some("11.0")),
        ("Ⅲ", Some("3.0")),
        ("二百零三", Some("203.0")),
        ("一点一", Some("1.1")),
        ("一点四五", Some("1.45")),
        ("零零点零零二八五", Some("0.00285")),
        ("八千七", Some("8700.0")),
        ("壹百万", Some("1000000.0")),
        ("一百零四万", Some("1040000.0")),
        ("负一百二十万", Some("-1200000.0")),
        ("负三点一四一五九二六", Some("-3.1415926")),
        ("负一百", Some("-100.0")),
        ("十一亿零九千零三十二", Some("1.100009032E9"))
      )

      it("schema eq") {
        testCases.foreach{
          case (query, target) =>
            val answers = analyze(query, testContext,	testOptions.copy(targets = Set(Numeral), full = false))
            answers(0).token.value match {
              case data: NumeralValue  => data.schema shouldBe target
              case _ => true shouldBe (false)
            }
        }
      }
    }

    describe("OrdinalSchemaTest") {
      val testCases = List(
        ("第五个", Some("5")),
        ("第二", Some("2")),
        ("第九十一", Some("91"))
      )

      it("schema eq") {
        testCases.foreach{
          case (query, target) =>
            val answers = analyze(query, testContext,	testOptions.copy(targets = Set(Ordinal), full = false))
            answers(0).token.value match {
              case data: OrdinalData  => data.schema shouldBe target
              case _ => true shouldBe (false)
            }
        }
      }
    }

    describe("FractionSchemaTest") {
      val testCases = List(
        ("三分之1", Some("1.0/3.0")),
        ("4分之3", Some("3.0/4.0")),
        ("百分之二十五", Some("25.0/100.0")),
        ("千分之二", Some("2.0/1000.0")),
        ("万分之一", Some("1.0/10000.0")),
        ("一半", Some("50.0/100.0")),
        ("50%", Some("50.0/100.0")),
        ("负三分之二", Some("-2.0/3.0")),
        ("负12/17", Some("-12.0/17.0"))
      )

      it("schema eq") {
        testCases.foreach{
          case (query, target) =>
            val answers = analyze(query, testContext,	testOptions.copy(targets = Set(Fraction), full = false))
            answers(0).token.value match {
              case data: FractionData  => data.schema shouldBe target
              case _ => true shouldBe (false)
            }
        }
      }
    }

    describe("NumberPrecisionTest") {
      val testCases = List(
        ("一点八四", 2),
        ("1.33333", 5),
        ("0.77", 2),
        ("1.1", 1),
        ("三十", 0),
        ("-1.83", 2),
        ("-4.44144", 5)
      )

      it("precision eq") {
        testCases.foreach{
          case (query, target) =>
            val answers = analyze(query, testContext,	testOptions.copy(targets = Set(Numeral), full = false))
            answers(0).token.value match {
              case data: NumeralValue  => data.precision shouldBe target
              case _ => true shouldBe (false)
            }
        }
      }
    }

  }
}
