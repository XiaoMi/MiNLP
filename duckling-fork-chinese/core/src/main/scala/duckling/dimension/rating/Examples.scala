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

package duckling.dimension.rating

import duckling.dimension.numeral.{DoubleSideIntervalValue, NumeralValue, OpenIntervalValue}
import duckling.dimension.time.enums.IntervalDirection._
import duckling.dimension.time.enums.IntervalType.Closed
import duckling.dimension.DimExamples
import duckling.Types

trait Examples extends DimExamples {

  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (NumeralValue(8.5), List("评分8点5分", "8.5分", "评分8.5")),
    (OpenIntervalValue(8.5, After), List("评分在8.5分以上", "8.5分以上", "评分大于八点五")),
    (OpenIntervalValue(4, Before), List("评分在4分以下")),
    (OpenIntervalValue(7, After), List("评分超过七分")),
    (OpenIntervalValue(9, Before), List("评分九点零以下")),
    (
      DoubleSideIntervalValue(7, 8.5, rightType = Closed),
      List("评分在7到8.5分", "评分在7到8.5", "评分在7到8.5分之间")
    )
  )
}
