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

package duckling

import org.scalatest.{FunSpec, Matchers}

import duckling.analyzer.HanlpAnalyzer

class DocumentTest extends FunSpec with Matchers {

  describe("DocumentTest") {

    it("should fromText") {
      val result = List(1, 1, 3, 3, 4, 5, 6, 7, 8, 9, 10, 12)
      Document
        .fromText(" a document ")
        .firstNonAdjacent should contain theSameElementsInOrderAs result
    }

    it("should phrase") {
      val analyzer = new HanlpAnalyzer()
      val doc = Document.fromLang(analyzer.analyze("湖南长沙"))
      doc.phrase(1, 2) shouldBe "长沙"
    }

  }
}
