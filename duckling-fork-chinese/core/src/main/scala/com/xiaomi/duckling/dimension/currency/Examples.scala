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

package com.xiaomi.duckling.dimension.currency

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.quantity.QuantityValue

trait Examples extends DimExamples {

  override val pairs: List[(ResolvedValue, List[String])] = List(
    (99.99, List("九十九元九角九分", "九十九块九毛九")),
    (99.9, List("九十九块九")),
    (9.9, List("九块九", "九元九角")),
    (9.09, List("九元零九分")),
    (0.99, List("九角九分", "九毛九")),
    (0.90, List("九毛钱")),
    (0.090, List("九分钱")),
    (2.99, List("两块九毛九")),
    (2.99, List("RMB两块九毛九", "两块九毛九RMB"))
  ).map { case (v, list) => (QuantityValue(v, "元", "货币:CNY"), list) }
}
