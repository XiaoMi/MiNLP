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

package duckling.dimension.place

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.place.Types.PlaceOne
import duckling.dimension.Dimension

case object Place extends Dimension with Rules with Examples {
  override val name: String = "Place"

  override val enableAnalyzer: Boolean = true
}

/**
  *
  * @param candidates   地点列表
  * @param isBirthPlace 是否是出生地，比如南阳人
  */
case class PlaceData(candidates: List[PlaceOne], isBirthPlace: Boolean = false, level: Int = 1, texts: Option[List[String]] = None)
  extends Resolvable
    with ResolvedValue {
  override def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)] = {
    (this, false)
  }
}
