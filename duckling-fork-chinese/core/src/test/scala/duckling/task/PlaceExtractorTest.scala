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

package duckling.task

import org.scalatest.{FunSpec, Matchers}

class PlaceExtractorTest extends FunSpec with Matchers {

  describe("PlaceExtractorTest") {
    it("testExtract") {
      PlaceExtractor.extract("湖南长沙") should contain("中华人民共和国/湖南省/长沙市")
      PlaceExtractor.extract("中山无极（今河北省无极县）") should contain("中华人民共和国/河北省/石家庄市/无极县")
      PlaceExtractor.extract("美国中山") should contain("美利坚合众国")
    }
  }
}
