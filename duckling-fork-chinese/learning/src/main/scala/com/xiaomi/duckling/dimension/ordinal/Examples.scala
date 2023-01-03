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

package com.xiaomi.duckling.dimension.ordinal

import com.xiaomi.duckling.Types
import com.xiaomi.duckling.dimension.{Dimension, DimExamples}

object Examples extends DimExamples {

  override def pairs: List[(Types.ResolvedValue, List[String])] =
    List((7, List("第七", "第七个")), (11, List("第十一")), (91, List("第九十一"))).map {
      case (expected, texts) => (OrdinalData(expected), texts)
    }

  override val dimension: Dimension = Ordinal
}
