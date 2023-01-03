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

package com.xiaomi.duckling.dimension.time.date

import com.xiaomi.duckling.Types
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.numeral.seq.DigitSequence
import com.xiaomi.duckling.dimension.time.duration.Duration

case object Date extends Dimension with Rules {
  override val name: String = "Date"

  override val dimDependents: List[Dimension] = List(Numeral, DigitSequence, Duration)

  override def dimRules: List[Types.Rule] = super.dimRules ++ LunarDates.rules
}
