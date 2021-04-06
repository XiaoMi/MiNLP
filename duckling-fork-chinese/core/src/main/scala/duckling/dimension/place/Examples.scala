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

import duckling.dimension.place.Types._
import duckling.dimension.DimExamples
import duckling.Types.ResolvedValue

trait Examples extends DimExamples {

  private def one(s: String) = getPlaceByName(s).head

  val list = List(
    ((List(one("当阳市")), false), List("湖北当阳", "当阳市", "湖北省当阳市", "当阳县")),
    ((List(one("湖北省")), false), List("湖北 省", "湖北", "湖北省")),
    ((List(one("市南区")), false), List("山东省青岛市市南区")),
    ((List(one("台南市")), false), List("台湾台南县")),
    ((List(one("诸城市")), true), List("山东诸城人")),
    ((List(one("无极县")), false), List("河北省无极县"))
  )

  override def pairs: List[(ResolvedValue, List[String])] = list.map {
    case ((c, i), texts) => (PlaceData(c, i), texts)
  }
}
