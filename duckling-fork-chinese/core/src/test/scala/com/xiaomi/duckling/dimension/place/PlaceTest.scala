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

package com.xiaomi.duckling.dimension.place

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types.Answer
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

class PlaceTest extends FunSpec with Matchers with TableDrivenPropertyChecks {
  val options = testOptions.copy(targets = Set(Place))

  def placeAnalyze(sentence: String): Answer = {
    val answers = analyze(sentence, testContext, options)
    answers.head
  }

  describe("PlaceTest") {

    it("simple") {
      placeAnalyze("湖北省当阳市").token.value match {
        case PlaceData(cc, _, _, _) => println(cc); cc.size should be > 0
      }
    }
  }

  describe("PlaceExtendTest") {
    val testCases = Table(("query", "places"),
      ("中国", List("中华人民共和国")),
      ("北京", List("中华人民共和国/北京市")),
      ("内蒙古", List("中华人民共和国/内蒙古自治区")),
      ("香港", List("中华人民共和国/香港特别行政区")),
      ("湖北", List("中华人民共和国/湖北省")),
      ("长沙", List("中华人民共和国/湖南省/长沙市/长沙县", "中华人民共和国/湖南省/长沙市")),
      ("武汉", List("中华人民共和国/湖北省/武汉市")),
      ("洪山区", List("中华人民共和国/湖北省/武汉市/洪山区")),
      ("永城市", List("中华人民共和国/河南省/商丘市/永城市")),
      ("柘城县", List("中华人民共和国/河南省/商丘市/柘城县"))
    )

    it("extend eq") {
      forEvery(testCases) {
        case (query, target) =>
          val answers = analyze(query, testContext,	testOptions.copy(targets = Set(Place), full = false))
          answers.size should be > 0
          answers(0).token.value match {
            case data: PlaceData  => data.texts shouldBe Some(target)
            case _ => true shouldBe false
          }
      }
    }
  }
}
