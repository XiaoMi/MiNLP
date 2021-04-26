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

package com.xiaomi.duckling

import java.lang.{Enum => JEnum}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import org.json4s.{CustomSerializer, DefaultFormats, FieldSerializer, Formats, NoTypeHints, Serializer, TypeHints}
import org.json4s.JsonAST.JString

import com.xiaomi.duckling.dimension.time._
import com.xiaomi.duckling.Types.{Entity, ResolvedVal, Token}
import com.xiaomi.duckling.dimension.numeral.NumeralValue
import com.xiaomi.duckling.dimension.quantity.QuantityValue
import com.xiaomi.duckling.dimension.time.Types.{DuckDateTime, InstantValue}
import com.xiaomi.duckling.dimension.time.duration.DurationData
import com.xiaomi.duckling.dimension.time.enums.{Grain, IntervalDirection, IntervalType}
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.ordinal.OrdinalData
import com.xiaomi.duckling.dimension.place.PlaceData
import com.xiaomi.duckling.dimension.time.predicates.SeriesPredicate
import com.xiaomi.duckling.types.Node

object JsonSerde {

  private val node = FieldSerializer[Node]({
    case ("production" | "features", _) => None
    case ("children", Nil)              => None
  })

  private val token = FieldSerializer[Token]({
    case ("dim", dim: Dimension) => Some("dim", dim.name)
  })

  private val resolvedVal = FieldSerializer[ResolvedVal]({
    case ("dimension", _) => None
  })

  private val timeValue = FieldSerializer[TimeValue]({
    case ("tzSeries", _) => None
  })

  private val instantValue = FieldSerializer[InstantValue]({
    case ("datetime", datetime: DuckDateTime) => Some("datetime", datetime.toString)
    case ("grain", grain: Grain)               => Some("grain", grain.name())
  })

  private val entity = FieldSerializer[Entity]({
    case ("latent", false) => None
  })

  private val timeData = FieldSerializer[TimeData]({
    case ("timePred", f: SeriesPredicate) => Some("timePred", "Series")
  })

  // 避开精度问题
  private val numeralValue = FieldSerializer[NumeralValue]({
    case ("n", n: Double) => Some("n", math.round(n * 1000) / 1000.0)
  })

  private val quantityValue = FieldSerializer[QuantityValue]({
    case ("v", n: Double) => Some("v", math.round(n * 1000) / 1000.0)
  })

  private val durationData = FieldSerializer[DurationData]({
    case ("grain", g: Grain) => Some("grain", g.name())
  })

  // 忽略
  private val placeData = FieldSerializer[PlaceData]({
    case ("level", _) => None
  })

  private val ordinalData = FieldSerializer[OrdinalData]({
    case ("ge", _) => None
  })

  /**
    * json4s未发布的代码 [[https://github.com/json4s/json4s/blob/master/ext/src/main/scala/org/json4s/ext/JavaEnumSerializer.scala]]
    */
  class JavaEnumNameSerializer[E <: JEnum[E]](implicit ct: Manifest[E])
      extends CustomSerializer[E](
        _ =>
          ({
            case JString(name) =>
              ct.runtimeClass match {
                case clazz: Class[E] => JEnum.valueOf(clazz, name)
              }
          }, {
            case dt: E => JString(dt.name())
          })
      )

  /**
    * 避开对Dimension和函数字段的序列化
    */
  implicit val formats: Formats = DuckFormats +
    node +
    token +
    resolvedVal +
    timeValue +
    instantValue +
    entity +
    timeData +
    numeralValue +
    quantityValue +
    durationData +
    placeData +
    ordinalData

  object DuckFormats extends DefaultFormats {
    override val typeHintFieldName: String = "class"
    override val typeHints: TypeHints = NoTypeHints
    override val customSerializers: List[Serializer[_]] =
      List(
        new JavaEnumNameSerializer[Grain](),
        new JavaEnumNameSerializer[IntervalType](),
        new JavaEnumNameSerializer[IntervalDirection]()
      )
  }
}
