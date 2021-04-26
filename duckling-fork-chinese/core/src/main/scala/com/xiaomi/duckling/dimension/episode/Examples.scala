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

package com.xiaomi.duckling.dimension.episode

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.quantity.QuantityValue

trait Examples extends DimExamples {
  override def pairs: List[(ResolvedValue, List[String])] = List(
    (QuantityValue(-1, "集", "集"), List("倒数第一集", "最后一集")),
    (QuantityValue(3, "集", "集"), List("第三集", "第三期", "第三回")),
    (QuantityValue(111, "集", "集"), List("第一百一十一期", "111集"))
  )
}
