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

package duckling.dimension.numeral.seq

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.DimRules
import duckling.dimension.matcher.Prods.singleRegexMatch
import duckling.dimension.numeral.{CapitalCNDigit, CNDigit}
import duckling.dimension.numeral.Prods.integerMap

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
