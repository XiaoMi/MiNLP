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

package com.xiaomi.duckling.dimension.time.duration

import com.xiaomi.duckling.Types.{Context, Options, Resolvable, ResolvedValue}
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.ordinal.Ordinal
import com.xiaomi.duckling.dimension.time.GrainWrapper
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.grain.TimeGrain
import com.xiaomi.duckling.dimension.time.unitNumber.UnitNumber

case object Duration extends Dimension with Rules with Examples {
  override val name: String = "Duration"

  override val dimDependents: List[Dimension] = List(TimeGrain, Numeral, UnitNumber)

  override val nonOverlapDims: List[Dimension] = List(Ordinal)
}

case class DurationData(value: Int, grain: Grain, latent: Boolean = false, override val schema: Option[String] = None)
    extends ResolvedValue
    with Resolvable {

  override def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)] = {
    (this, latent)
  }

  def +(o: DurationData): DurationData = {
    val g = if (grain < o.grain) grain else o.grain
    val d1 = withGrain(g, this)
    val d2 = withGrain(g, o)
    DurationData(d1.value + d2.value, g, schema = durationSchema(d1, d2))
  }

  override def toString: String = value.toString
}
