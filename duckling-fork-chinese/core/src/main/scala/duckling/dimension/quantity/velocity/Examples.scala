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

package duckling.dimension.quantity.velocity

import duckling.dimension.quantity.QuantityValue
import duckling.dimension.DimExamples
import duckling.Types

trait Examples extends DimExamples {
  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (QuantityValue(3, "千米每小时", "Velocity"), List("3千米每小时", "每小时3千米", "3公里每小时", "每小时3公里")),
    (QuantityValue(0.8, "米每秒", "Velocity"), List("0.8米每秒")),
    (QuantityValue(1, "米每秒", "Velocity"), List("每秒1米")),
    (QuantityValue(1.8, "英里每小时", "Velocity"), List("1.8英里每小时", "1.8迈", "1.8码")),
    (QuantityValue(4, "英尺每秒", "Velocity"), List("4英尺每秒", "每秒4英尺"))
  )

}
