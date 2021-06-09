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

package com.xiaomi.duckling.dimension.gender

import com.google.common.collect.ImmutableListMultimap

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.LexiconMatches
import com.xiaomi.duckling.engine.LexiconLookup.Dict

trait Rules extends DimRules {

  val dict = {
    val builder = ImmutableListMultimap.builder[String, String]()
    builder.put("女", "女")
    builder.put("女性", "女")
    builder.put("男", "男")
    builder.put("男性", "男")

    Dict(vocab = builder.build())
  }


  val rule =
    Rule(name = "gender", pattern = List(dict.lexicon), prod = tokens {
      case Token(_, LexiconMatches(_, n)) :: _ =>
        Token(Gender, GenderData(n))
    })
}
