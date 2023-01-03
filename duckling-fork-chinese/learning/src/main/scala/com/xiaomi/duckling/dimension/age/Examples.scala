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

package com.xiaomi.duckling.dimension.age

import com.xiaomi.duckling.dimension.{Dimension, DimExamples}
import com.xiaomi.duckling.dimension.numeral.{DoubleSideIntervalValue, NumeralValue, OpenIntervalValue}
import com.xiaomi.duckling.dimension.time.enums.IntervalDirection._
import com.xiaomi.duckling.dimension.time.enums.IntervalType.Closed

object Examples extends DimExamples {
  override val dimension: Dimension = Age

  override val pairs = List(
    (NumeralValue(3), List("三岁")),
    (OpenIntervalValue(3.5, After), List("三岁半以上")),
    (OpenIntervalValue(3.5, After), List("大于三岁半")),
    (OpenIntervalValue(3.5, Before), List("三岁半以下")),
    (DoubleSideIntervalValue(3, 5.5, rightType = Closed), List("三到五岁半")),
    (DoubleSideIntervalValue(3, 5, rightType = Closed), List("三到五岁"))
  )
}
