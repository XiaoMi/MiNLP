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

package com.xiaomi.duckling.dimension.season

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.{Dimension, DimExamples}
import com.xiaomi.duckling.dimension.quantity.QuantityValue

object Examples extends DimExamples {
  override def pairs: List[(ResolvedValue, List[String])] = List(
    (QuantityValue(-1, "季", "季"), List("倒数第一季", "最后一季")),
    (QuantityValue(3, "季", "季"), List("第三季", "第三部", "3季", "三部"))
  )

  override val dimension: Dimension = Season
}
