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

package com.xiaomi.duckling

import com.xiaomi.duckling.Types.{Range, RankOptions}
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.types.Node

package object dimension {
  def blankNode(s: Int, e: Int) = Node(Range(s, e), null, null, null, null)

  private val rankOptions = RankOptions(winnerOnly = false)

  def answerSize(sentence: String, targets: Set[Dimension]): Int = {
    val options = testOptions.copy(rankOptions = rankOptions, targets = targets)
    val answers = analyze(sentence, testContext, options.copy(rankOptions = rankOptions, full = true))
    answers.size
  }
}
