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

package com.xiaomi.duckling.dimension.time.rule

import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.enums.Grain.Hour
import com.xiaomi.duckling.dimension.time.TimeData
import com.xiaomi.duckling.dimension.time.form.TimeOfDay
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.{SequencePredicate, TimeDatePredicate}
import com.xiaomi.duckling.ranking.Testing
import com.xiaomi.duckling.UnitSpec

class FuzzyDayIntervalsTest extends UnitSpec {

  describe("FuzzyDayIntervalsTest") {

    it("should rule24OClockOfDay - 今天晚上12点") {
      val h24 = TimeData(
        timePred = TimeDatePredicate(hour = (false, 24)),
        timeGrain = Hour,
        form = TimeOfDay(24, false)
      )

      val pred = SequencePredicate(List(today, h24))
      val td = TimeData(pred, timeGrain = Hour)

      println(td.resolve(Testing.testContext, Testing.testOptions))
    }

  }
}
