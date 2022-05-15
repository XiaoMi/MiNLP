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

package com.xiaomi.duckling.dimension.time.grain

import com.xiaomi.duckling.{Api, UnitSpec}
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.dimension.time.duration.Duration
import com.xiaomi.duckling.ranking.Testing


class TimeGrainTest extends UnitSpec {
  describe("TimeGrain") {
    it("正则匹配周") {
      val r = WeekPattern.r
      r.findFirstIn("上周") should contain("周")
      r.findFirstIn("上周一") shouldBe None
      r.findFirstIn("上周天气") should contain("周")
    }

    it("负例") {
      val answers = Api.analyze("2017年", Testing.testContext, Testing.testOptions.copy(targets = Set(Duration, Time)))
      answers.size shouldBe 1
      answers.head.dim shouldBe Time
    }
  }
}
