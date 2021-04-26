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

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types.Token
import com.xiaomi.duckling.ranking.Testing.{examples, testContext, testOptions, Corpus, Example}
import com.xiaomi.duckling.ranking.Types.{DiscreteFeature, Feature}
import com.xiaomi.duckling.types.Node

/**
  * GADT for differentiating between dimensions
  * Each dimension should have its own constructor and provide the data structure
  * for its parsed data
  */
trait Dimension extends DimRules with DimExamples {
  val name: String
  val dimDependents: List[Dimension] = Nil
  val nonOverlapDims: List[Dimension] = Nil

  def overlap(token: Token): Boolean = true

  def constraints(doc: Document, node: Node): Boolean = true

  def enableAnalyzer: Boolean = false

  def combinationFeatures(node: Node): List[Feature] = {
    List(DiscreteFeature(node.rule.get))
  }

  lazy val allExamples: List[Example] = pairs.flatMap {
    case (expected, texts) => examples(expected, texts, enableAnalyzer = enableAnalyzer)
  }

  lazy val corpus: Corpus = (testContext, testOptions.copy(targets = Set(this)), allExamples)
}

object Dimension {
  def dimDependents(dim: Dimension): Set[Dimension] = {
    val deps = dim.dimDependents.flatMap(d => d.dimDependents.map(Dimension.dimDependents))
    val firstLevel = (dim +: dim.dimDependents).toSet

    if (deps.nonEmpty) deps.reduce(_ ++ _) ++ firstLevel
    else firstLevel
  }

  def dimDependents(dims: Iterable[Dimension]): Set[Dimension] = {
    dims.flatMap(dimDependents).toSet
  }
}
