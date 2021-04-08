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

package duckling.dimension.phoneno

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.matcher.Prods.regexMatch
import duckling.dimension.DimRules

trait Rules extends DimRules {
  private val exclude = List('.', ' ', '-', '\t', '(', ')')

  private def w(s: String): Option[String] = {
    if (s.length == 0) None
    else Some(s)
  }

  val rulePhoneNumber = Rule(
    name = "phone number",
    pattern = List(
      ("(?:\\(?(\\+|00)(\\d{1,2})\\)?[\\s\\-\\.]*)?" + // area code - (+86) / 0086
        "((?=[-\\d()\\s\\.]{6,16}(?:\\s*e?xt?\\.?\\s*(?:\\d{1,20}))?(?:[^\\d]+|$))(?:[\\d(]{1,20}(?:[-)\\s\\.]*\\d{1,20}){0,20}){1,20})" + // nums
        "(?:\\s*(e?xt?|转)\\.?\\s*(\\d{1,20}))?"
        ).regex
    ),
    prod = regexMatch {
      case _ :: _ :: code :: nums :: _ :: ext :: _ =>
        val v = nums.filter(c => !exclude.contains(c))
        token(v, area = w(code), ext = w(ext))
    }
  )
}
