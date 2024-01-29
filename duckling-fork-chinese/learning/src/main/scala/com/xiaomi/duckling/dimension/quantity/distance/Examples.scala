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

package com.xiaomi.duckling.dimension.quantity.distance

import com.xiaomi.duckling.Types
import com.xiaomi.duckling.dimension.{Dimension, DimExamples}
import com.xiaomi.duckling.dimension.quantity.QuantityValue

object Examples extends DimExamples {
  override def pairs: List[(Types.ResolvedValue, List[String])] = List(
    (QuantityValue(3000, "米", "Distance", originValue = Some(3), originUnit = Some("千米")), List("3千米", "3km", "3Km", "3KM")),
    (QuantityValue(3000, "米", "Distance", originValue = Some(3), originUnit = Some("公里")), List("三公里")),
    (QuantityValue(3000, "米", "Distance", originValue = Some(3000), originUnit = Some("米")), List("3000米", "3000m", "3000M")),
    (QuantityValue(1000, "米", "Distance", originValue = Some(2), originUnit = Some("里")), List("二里地", "2里")),
    (QuantityValue(16093.1, "米", "Distance", originValue = Some(10), originUnit = Some("英里")), List("十英里", "10英里")),
    (QuantityValue(5559, "米", "Distance", originValue = Some(3), originUnit = Some("海里")), List("三海里", "3海里")),
    (QuantityValue(0.1, "米", "Distance", originValue = Some(1), originUnit = Some("分米")), List("一分米", "1dm", "1DM")),
    (QuantityValue(0.1, "米", "Distance", originValue = Some(10), originUnit = Some("厘米")), List("十厘米", "10cm", "10CM")),
    (QuantityValue(0.003, "米", "Distance", originValue = Some(3), originUnit = Some("毫米")), List("三毫米", "3mm", "3MM")),
    (QuantityValue(3.0E-6, "米", "Distance", originValue = Some(3), originUnit = Some("微米")), List("3微米", "3µm", "3µM")),
    (QuantityValue(3.0E-9, "米", "Distance", originValue = Some(3), originUnit = Some("纳米")), List("3纳米", "3nm", "3NM")),
    (QuantityValue(1.0E-11, "米", "Distance", originValue = Some(10), originUnit = Some("皮米")), List("十皮米", "10pm", "10PM")),
    (QuantityValue(6.66, "米", "Distance", originValue = Some(2), originUnit = Some("丈")), List("两丈")),
    (QuantityValue(0.999, "米", "Distance", originValue = Some(3), originUnit = Some("尺")), List("三尺")),
    (QuantityValue(0.304794, "米", "Distance", originValue = Some(1), originUnit = Some("英尺")), List("一英尺")),
    (QuantityValue(0.0999, "米", "Distance", originValue = Some(3), originUnit = Some("寸")), List("三寸")),
    (QuantityValue(0.025399, "米", "Distance", originValue = Some(1), originUnit = Some("英寸")), List("1英寸")),
    (QuantityValue(0.914383, "米", "Distance", originValue = Some(1), originUnit = Some("码")), List("1码")),
    (QuantityValue(9.46E+16, "米", "Distance", originValue = Some(10), originUnit = Some("光年")), List("10光年"))
  )

  override val dimension: Dimension = Distance
}
