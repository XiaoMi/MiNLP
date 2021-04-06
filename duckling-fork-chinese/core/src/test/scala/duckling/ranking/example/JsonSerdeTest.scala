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

package duckling.ranking.example

import org.json4s.jackson.Serialization.write
import org.scalatest.{FunSpec, Matchers}

import duckling.{Api, Types}
import duckling.JsonSerde._
import duckling.dimension.numeral.Numeral
import duckling.ranking.Testing

class JsonSerdeTest extends FunSpec with Matchers {

  describe("JsonSerdeTest") {
    it("should serialize entity") {
      val e: List[Types.Entity] =
        Api.parseEntities("123", Testing.testContext, Testing.testOptions.copy(targets = Set(Numeral)))
      noException should be thrownBy write(e)
    }
  }
}
