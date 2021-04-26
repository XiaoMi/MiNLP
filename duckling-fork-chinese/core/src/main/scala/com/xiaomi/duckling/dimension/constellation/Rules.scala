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

package com.xiaomi.duckling.dimension.constellation

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.matcher.Prods._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._

trait Rules extends DimRules {

  private lazy val lexemeMap = lexicons.flatMap(l => l.lexeme.map((_, l.target))).toMap

  private val pattern = lexicons.flatMap(_.lexeme).mkString("(", "|", ")")

  lazy val rule =
    Rule(name = "constellation", pattern = List(s"${pattern}座*".regex), prod = regexMatch {
      case whole :: group :: _ => token(lexemeMap(group), !whole.endsWith("座"))
    })

  def token(w: String, latent: Boolean): Token = Token(Constellation, ConstellationData(w, latent))
}
