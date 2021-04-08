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

package duckling.dimension.temperature

import duckling.dimension.DimExamples
import duckling.Types
import duckling.dimension.quantity.QuantityValue

trait Examples extends DimExamples {
  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (QuantityValue(30, "C", "温度"), List("摄氏30度", "30摄氏度")),
    (QuantityValue(13, "C", "温度"), List("十三度", "十三°", "13°C")),
    (QuantityValue(12.5, "C", "温度"), List("12点5度", "12.5摄氏度")),
    (QuantityValue(-25.3, "C", "温度"), List("零下25.3摄氏度", "零下25.3°C", "负的二十五点3度")),
    (QuantityValue(21, "F", "温度"), List("华氏21度", "2十一华氏度")),
    (QuantityValue(+22.6, "F", "温度"), List("华氏22点6度", "二十二点6华氏度")),
    (QuantityValue(-13, "F", "温度"), List("零下华氏十3度", "华氏零下十3度", "负的13华氏度"))
  )
}
