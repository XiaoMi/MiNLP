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

package com.xiaomi.duckling.ranking

import java.util.{Map => JMap}

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Resources
import com.xiaomi.duckling.Types.Answer
import com.xiaomi.duckling.ranking.Bayes.Classifier
import com.xiaomi.duckling.ranking.Types.BagOfFeatures
import com.xiaomi.duckling.types.Node

class NaiveBayesRank(modelResource: String) extends LazyLogging {

  type Classifiers = JMap[String, Classifier]

  private val classifiers: Classifiers = try {
    Resources.inputStream(modelResource)(in => KryoSerde.loadSerializedResource(in, classOf[Classifiers]))
  } catch {
    case t: Throwable =>
      logger.error(s"load model failed ${t.getMessage}", t.getCause)
      throw t
  }

  def score(answer: Answer): Answer = {
    answer.copy(score = score(answer.token.node))
  }

  def score(node: Node): Double = {
    node match {
      case Node(_, _, _, None, _, _) => 0.0
      case Node(_, _, children, Some(rule), _, _) =>
        classifiers.get(rule) match {
          case null => 0.0
          case c =>
            val feats = NaiveBayesRank.extractFeatures(node)
            val childSum = children.map(score).sum
            if (feats.nonEmpty) Bayes.classify(c, feats)._2 + childSum
            else childSum
        }
    }
  }
}

object NaiveBayesRank {
  /**
   * -- | Feature extraction
   * -- | Features:
   * -- | 1) Concatenation of the names of the rules involved in parsing `Node`
   * -- | 2) Concatenation of the grains for time-like dimensions
   *
   * @param node
   * @return
   */
  def extractFeatures(node: Node): BagOfFeatures = {
    val rules = node.children.flatMap(_.rule)
    Map(rules.mkString("/") -> 1)
  }
}