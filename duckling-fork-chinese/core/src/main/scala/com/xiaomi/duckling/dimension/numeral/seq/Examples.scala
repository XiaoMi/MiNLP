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

package com.xiaomi.duckling.dimension.numeral.seq

import com.xiaomi.duckling.dimension.DimExamples

trait Examples extends DimExamples {
  override val pairs =
    List(
      (DigitSequenceData("0111", false), List("0111")),
      (DigitSequenceData("011", true, "零一幺"), List("零一幺")),
      (DigitSequenceData("001", true, "零零幺"), List("零零幺")),
      (DigitSequenceData("001", false), List("001")),
      (DigitSequenceData("114", true, "幺幺四"), List("幺幺四"))
    ).map {
      case (expected, texts) => (expected, texts)
    }
}
