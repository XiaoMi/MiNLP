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

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.{Resources, Rules}
import com.xiaomi.duckling.Types.{conf, Answer, Rule}
import com.xiaomi.duckling.dimension.RuleSets
import com.xiaomi.duckling.ranking.Bayes.Classifier
import com.xiaomi.duckling.ranking.Types.BagOfFeatures
import com.xiaomi.duckling.types.Node

object NaiveBayesRank extends LazyLogging {

  type Classifiers = JMap[String, Classifier]

  private val dimPath = "model.bayes.dims"
  private val modelPath = "model.bayes.file"

  private val dims =
    if (conf.hasPath(dimPath)) {
      conf.getStringList(dimPath).asScala.map(s => RuleSets.namedDimensions(s.toLowerCase)).toList
    } else {
      throw new IllegalArgumentException("no dimension specified, ignore")
    }

  val rules: List[Rule] = Rules.rulesFor(null, dims.toSet)

  lazy val classifiers: Classifiers = {
    try {
      val path =
        if (conf.hasPathOrNull(modelPath)) conf.getString(modelPath)
        else "file_not_found"
      logger.info(s"read NaiveBayes model from resource: $path")
      Resources.inputStream(path)(in => KryoSerde.loadSerializedResource(in, classOf[Classifiers]))
    } catch {
      case t: Throwable =>
        logger.warn(s"load model failed ${t.getMessage}", t.getCause)
        throw t
    }
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
            val feats = extractFeatures(node)
            val childSum = children.map(score).sum
            if (feats.nonEmpty) Bayes.classify(c, feats)._2 + childSum
            else childSum
        }
    }
  }

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
