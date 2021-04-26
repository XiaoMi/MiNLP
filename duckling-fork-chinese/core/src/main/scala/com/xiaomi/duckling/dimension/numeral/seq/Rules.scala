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

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.singleRegexMatch
import com.xiaomi.duckling.dimension.numeral.Prods.integerMap
import com.xiaomi.duckling.dimension.numeral.{CNDigit, CapitalCNDigit}

trait Rules extends DimRules {
  val cn = Rule(
    name = "cn digit sequence",
    pattern = List(s"($CNDigit{2,}|$CapitalCNDigit{2,})".regex),
    prod = singleRegexMatch {
      case text =>
        val seq = text.map(c => integerMap(c.toString)).mkString("")
        token(seq, zh = true, raw = text)
    }
  )

  val cn1less = Rule(
    name = "cn digit sequence",
    pattern = List(s"($CNDigit{2,}(?=$CNDigit)|$CapitalCNDigit{2,}(?=$CapitalCNDigit))".regex),
    prod = singleRegexMatch {
      case text =>
        val seq = text.map(c => integerMap(c.toString)).mkString("")
        token(seq, zh = true, raw = text)
    }
  )

  val arabic = Rule(
    name = "arabic digit sequence",
    pattern = List("[0-9]{2,}".regex),
    prod = singleRegexMatch {
      case text => token(text, zh = false)
    }
  )

}
