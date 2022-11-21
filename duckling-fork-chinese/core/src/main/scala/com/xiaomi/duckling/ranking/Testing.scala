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

package com.xiaomi.duckling.ranking

import com.typesafe.scalalogging.LazyLogging
import org.json4s.FieldSerializer
import org.json4s.jackson.Serialization.write

import java.time.{LocalDateTime, ZonedDateTime}
import java.util.Locale

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.place.PlaceData
import com.xiaomi.duckling.dimension.quantity.QuantityData
import com.xiaomi.duckling.dimension.time.TimeValue
import com.xiaomi.duckling.{Document, JsonSerde}

object Testing extends LazyLogging {
  type TestPredicate = (Document, Context) => ResolvedToken => Boolean
  type Example = (Document, TestPredicate, Int)
  type Corpus = (Context, Options, List[Example])
  type NegativeCorpus = (Context, Options, List[String])

  /**
   * 有一些字段构造起来比较困难，或者不用比较，可以忽略掉
   */
  val sTimeValue = FieldSerializer[TimeValue]({
    case ("values", _) => None
    case ("simple", _) => None
  })

  val sNumeralValue = FieldSerializer[NumeralValue]({
    case ("precision", _) => None
  })

  val sPlaceData = FieldSerializer[PlaceData]({
    case ("texts", _) => None
    case ("level", _) => None
  })

  val sQuantityValue = FieldSerializer[QuantityData]({
    case ("isLatent", _) => None
  })

  implicit val formats = JsonSerde.formats + sTimeValue + sNumeralValue + sPlaceData + sQuantityValue

  val testContext: Context =
    Context(
      locale = Locale.CHINA,
      referenceTime = ZonedDateTime.of(LocalDateTime.of(2013, 2, 12, 4, 30, 0), ZoneCN)
    )

  val testOptions: Options = Options(full = true, debug = true)

  def examples(output: ResolvedValue,
               texts: List[String],
               weight: Int = 1,
               enableAnalyzer: Boolean = false): List[Example] = {
    texts.map(text => (Document.fromText(text, enableAnalyzer = enableAnalyzer), simpleCheck(output), weight))
  }

  def simpleCheck(value: ResolvedValue): TestPredicate =
    (doc: Document, _: Context) =>
      (resolvedToken: ResolvedToken) => {
        val expected = write(value)
        val actual = write(resolvedToken.value)
        val equals = expected == actual
        if (!equals && testOptions.debug) {
          logger.debug(s"checking: ${doc.rawInput}")
          logger.debug(s"expected ${if (expected == actual) "=" else "!="} actual")
          logger.debug(s"expected: $expected")
          logger.debug(s"actual  : $actual")
        }
        equals
    }
}
