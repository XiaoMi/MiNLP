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

package com.xiaomi.duckling.dimension.temperature

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types.Answer
import com.xiaomi.duckling.dimension.answerSize
import com.xiaomi.duckling.dimension.quantity.QuantityValue
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.UnitSpec

class TemperatureTest extends UnitSpec {

  val options = testOptions.copy(targets = Set(Temperature))

  def temperatureAnalyze(sentence: String): Answer = {
    analyze(sentence, testContext, options).head
  }

  describe("TemperatureTest") {
    it("simple") {
      temperatureAnalyze("2十一°F").token.value match {
        case QuantityValue(v, unit, dim, _, _) =>
          (v, unit, dim) shouldBe ((21.0, "F", "温度"))
      }
    }
    it("answer size") {
      answerSize("12点五度", Set(Temperature)) shouldBe 1
    }
  }

  describe("TemperatureSchemaTest") {
    val testCases = List(
      ("摄氏30度", Some("30.0C")),
      ("十三度", Some("13.0C")),
      ("12.5摄氏度", Some("12.5C")),
      ("零下25.3度", Some("-25.3C")),
      ("华氏21度", Some("21.0F")),
      ("华氏零下十五度", Some("-15.0F")),
      ("华氏22点6度", Some("22.6F")),
      ("23度半", Some("23.5C")),
      ("负22度半", Some("-22.5C")),
      ("华氏22度半", Some("22.5F")),
      ("摄氏22度半", Some("22.5C")),
      ("华氏零下三十一度半", Some("-31.5F")),
      ("零下摄氏三十一度半", Some("-31.5C"))
    )

    it("schema eq") {
      testCases.foreach {
        case (query, target) =>
          val answers = analyze(query, testContext, options)
          answers(0).token.value match {
            case data: QuantityValue => data.schema shouldBe target
            case _ => true shouldBe (false)
          }
      }
    }
  }

}
