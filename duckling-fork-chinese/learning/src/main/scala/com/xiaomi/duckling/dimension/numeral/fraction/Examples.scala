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

package com.xiaomi.duckling.dimension.numeral.fraction

import com.xiaomi.duckling.Types
import com.xiaomi.duckling.dimension.{Dimension, DimExamples}

object Examples extends DimExamples {

  override def pairs: List[(Types.ResolvedValue, List[String])] =
    List(
      (0.6, 60.0, 100.0, 0, 0, Some(2), List("百分之六十")),
      (0.7, 70.0, 100.0, 0, 0, Some(2), List("70%")),
      (0.6, 600.0, 1000.0, 0, 0, Some(3), List("千分之六百")),
      (0.75, 3.0, 4.0, 0, 0, None, List("4分之3", "四分之3", "3/4")),
      (0.5, 64.0, 128.0, 0, 0, None, List("一百二十八分之64")),
      (0.5, 50.0, 100.0, 0, 0, Some(2), List("一半", "50%", "五十%")),
      (0.5, 60.0, 120.0, 0, 0, None, List("六十/一百二十")),
      (-0.6, 60.0, -100.0, 0, 0, Some(2), List("负百分之六十", "负的百分之六十")),
      (-1.0, 100.0, -100.0, 0, 0, Some(2), List("负百分之百", "负的百分之百", "负的百分之一百")),
      (1.0, 100.0, 100.0, 0, 0, Some(2), List("百分之一百", "百分百", "百分之百")),
      (0.5, 1.5, 3.0, 1, 0, None, List("三分之一点五", "1.5/3")),
      (0.1562, 15.62, 100.0, 2, 0, Some(4), List("百分之十五点六二", "15.62%")),
      (0.2, 2.5, 12.5, 1, 1, None, List("十二点五分之二点五", "2.5/12.5"))
    ).map {
      case (expected, numerator, denominator, numeratorPrecision, denominatorPrecision, precision, texts) => (FractionData(expected, numerator, denominator, numeratorPrecision, denominatorPrecision, precision), texts)
    }

  override val dimension: Dimension = Fraction
}
