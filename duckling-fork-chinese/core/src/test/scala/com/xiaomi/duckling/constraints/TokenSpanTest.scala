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

package com.xiaomi.duckling.constraints

import com.xiaomi.duckling.{Api, Types, UnitSpec}
import com.xiaomi.duckling.Types.{Context, Options}
import com.xiaomi.duckling.dimension.place.Place
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.types.{LanguageInfo, TokenLabel}

class TokenSpanTest extends UnitSpec {

  describe("TokenSpan") {
    it("testIsValid") {
      val lang = LanguageInfo("下周日本的天气", Array(
        TokenLabel(1, "下周", 0, 2, "O"),
        TokenLabel(2, "日本", 2, 4, "O"),
        TokenLabel(3, "的", 4, 5, "O"),
        TokenLabel(4, "天气", 5, 7, "O")
      ))

      val answers = Api.analyze(lang, new Context(), Options(withLatent = false, targets = Set(Time, Place)))
      answers.size shouldBe 2
      answers(0).range shouldBe Types.Range(0, 2)
      answers(0).dim shouldBe Time
      answers(1).range shouldBe Types.Range(2, 4)
      answers(1).dim shouldBe Place
    }
  }
}
