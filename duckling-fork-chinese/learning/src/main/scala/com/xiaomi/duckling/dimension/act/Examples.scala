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

package com.xiaomi.duckling.dimension.act

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.quantity.QuantityValue
import com.xiaomi.duckling.dimension.{DimExamples, Dimension}

object Examples extends DimExamples {
  override def pairs: List[(ResolvedValue, List[String])] = List(
    (QuantityValue(-1, "场", "场"), List("倒数第一场", "最后一番", "最新一场")),
    (QuantityValue(3, "场", "场"), List("第三场", "第三番", "3场", "三弹"))
  )

  override val dimension: Dimension = Act
}
