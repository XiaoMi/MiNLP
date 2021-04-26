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

package com.xiaomi.duckling.dimension.quantity

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}

trait Rules extends DimRules {
  // 计算器暂时无需求，只开个头即可

  val units = List("千焦", "千瓦时", "千米", "千克", "两", "百帕", "千帕", "兆帕", "千米每秒", "千米每小时", "千瓦")

  val rule = Rule(
    name = "<number> unit",
    pattern = List(isDimension(Numeral).predicate, units.mkString("(", "|", ")").regex),
    prod = {
      case Token(Numeral, NumeralData(v, _, _, _, _, _)) :: Token(RegexMatch, GroupMatch(s :: _))
            :: _ =>
        Token(Quantity, QuantityData(v, s, "?"))
    }
  )

  val rules = List(rule)
}
