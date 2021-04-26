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

package com.xiaomi.duckling.dimension.url

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.regexMatch

trait Rules extends DimRules {

  private def opt(s: String): Option[String] = {
    if(s.isEmpty) None else Some(s)
  }

  val ruleURL = Rule(
    name = "url",
    pattern = List(
      "(([a-zA-Z]+)://)?(w{2,3}[0-9]*\\.)?(([\\w_-]+\\.)+[a-z]{2,4})(:(\\d+))?(/[^?\\s#]*)?(\\?[^\\s#]+)?(#[\\-,*=&a-zA-Z0-9]+)?".regex
    ),
    prod = regexMatch {
      case url :: _ :: protocol :: _ :: domain :: _ :: _ :: port :: path :: query :: _ =>
        token(url, domain, opt(protocol))
    }
  )

  val ruleLocalhost = Rule(
    name = "localhost",
    pattern = List("(([a-zA-Z]+)://)?localhost(:(\\d+))?(/[^?\\s#]*)?(\\?[^\\s#]+)?".regex),
    prod = regexMatch {
      case m :: _protocol :: _ :: _port :: _path :: _query :: _ =>
        token(m, "localhost", opt(_protocol))
    }
  )

  val ruleLocalURL = Rule(
    name = "local url",
    pattern = List("([a-zA-Z]+)://([\\w_-]+)(:(\\d+))?(/[^?\\s#]*)?(\\?[^\\s#]+)?".regex),
    prod = regexMatch {
      case m :: _protocol :: domain :: _ :: _port :: _path :: _query :: _ =>
        token(m, domain, opt(_protocol))
    }
  )
}
