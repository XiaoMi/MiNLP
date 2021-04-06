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

package duckling.dimension.numeral.multiple

import duckling.Types
import duckling.dimension.DimExamples
import duckling.dimension.numeral.NumeralValue

trait Examples extends DimExamples {
  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (MultipleData(8.5), List("8点5倍", "8.5倍")),
    (MultipleData(2.0), List("2倍", "两倍")),
    (MultipleData(101.0), List("一百零一倍", "101倍"))
  )
}
