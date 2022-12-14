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

package com.xiaomi.duckling.dimension

import com.xiaomi.duckling.dimension.numeral.multiple.Multiple
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.numeral.fraction.Fraction
import com.xiaomi.duckling.dimension.numeral.seq.DigitSequence
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.dimension.time.date.Date
import com.xiaomi.duckling.dimension.time.duration.Duration
import com.xiaomi.duckling.dimension.time.repeat.Repeat

class LiteDimensions extends Dimensions {
  override val dims: List[Dimension] = List(
    Date,
    DigitSequence,
    Duration,
    Fraction,
    Multiple,
    Numeral,
    Repeat,
    Time
  )
}
