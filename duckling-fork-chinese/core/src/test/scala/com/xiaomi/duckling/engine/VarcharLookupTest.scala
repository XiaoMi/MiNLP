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

package com.xiaomi.duckling.engine



import com.xiaomi.duckling.{Document, UnitSpec}
import com.xiaomi.duckling.Types.DefaultExcludes

class VarcharLookupTest extends UnitSpec {

  describe("VarcharLookupTest") {

    it("should lookupVar") {
      val doc = Document.fromText("教练")
      VarcharLookup.lookupVar(doc, 2, 20, 0, DefaultExcludes) should have size 1
      val doc2 = Document.fromText("零一二三四五六七八九") // (8+5)*4/2
      VarcharLookup.lookupVar(doc2, 2, 5, 1, DefaultExcludes) should have size 26
    }
  }
}
