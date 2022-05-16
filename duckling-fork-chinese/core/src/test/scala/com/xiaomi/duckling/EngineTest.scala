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

package com.xiaomi.duckling

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.engine.RegexLookup._
import com.xiaomi.duckling.types.Node

class EngineTest extends UnitSpec {

  describe("EngineTest") {

    it("Empty Regex Test") {
      lookupRegexAnywhere(Document.fromText("hey"), "()".r) should be(empty)
    }

    it("Unicode and Regex Test") {
      val doc = Document.fromText("中 $35")
      val re = "\\$([0-9]*)".r
      val nodes = lookupRegexAnywhere(doc, re)
      nodes should have size 1
      val expected = Node(
        range = Range(2, 5),
        token = Token(RegexMatch, GroupMatch(List("$35", "35"))),
        children = Nil,
        rule = None,
        production = null
      )
      nodes.head should be(expected)
    }
  }
}
