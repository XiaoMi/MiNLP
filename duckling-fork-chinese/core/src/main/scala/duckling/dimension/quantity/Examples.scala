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

package duckling.dimension.quantity

import duckling.dimension.DimExamples
import duckling.Types

trait Examples extends DimExamples {

  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (QuantityData(1, "千米", "?"), List("一千米")),
    (QuantityData(1, "千帕", "?"), List("一千帕")),
    (QuantityData(3000, "千瓦时", "?"), List("三千千瓦时")),
    (QuantityData(1, "两", "?"), List("一两"))
  )
}
