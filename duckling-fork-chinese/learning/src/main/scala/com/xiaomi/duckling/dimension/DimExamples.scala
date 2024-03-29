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

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.ranking.CorpusSets.{examples, Corpus, Example}
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

trait DimExamples {
  val dimension: Dimension

  def pairs: List[(ResolvedValue, List[String])]

  lazy val allExamples: List[Example] = pairs.flatMap {
    case (expected, texts) => examples(expected, texts, enableAnalyzer = true)
  }

  lazy val corpus: Corpus = (testContext, testOptions.copy(targets = Set(dimension)), allExamples)
}
