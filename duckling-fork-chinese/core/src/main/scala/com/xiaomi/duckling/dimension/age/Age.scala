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

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.{IntervalValue, Numeral}

case object Age extends Dimension with Rules {
  override val name: String = "Age"

  override val dimDependents: List[Dimension] = List(Numeral)
}

case class AgeData(interval: IntervalValue) extends Resolvable {
  override def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)] = {
    (interval, false)
  }
}
