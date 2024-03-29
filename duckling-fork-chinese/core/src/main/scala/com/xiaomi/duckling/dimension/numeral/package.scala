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

package com.xiaomi.duckling.dimension

import com.xiaomi.duckling.Types.{Options, Production, Token}

package object numeral {
  val CNDigit = "(零|幺|一|二|两|三|四|五|六|七|八|九)"
  val CapitalCNDigit = "(〇|壹|贰|叁|肆|伍|陆|柒|捌|玖)"


  type NumeralProd = PartialFunction[(NumeralOptions, List[Token]), Option[Token]]

  def opt(nopt: NumeralProd): Production = {
    case (options: Options, tokens: List[Token]) if nopt.isDefinedAt((options.numeralOptions, tokens)) =>
      nopt(options.numeralOptions, tokens)
    case _ => None
  }
}
