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

package duckling.ranking

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._

import org.json4s.jackson.Serialization.{read, writePretty}

import com.typesafe.scalalogging.LazyLogging

import duckling.{Resources, Rules}
import duckling.Types.{conf, Answer, Rule}
import duckling.ranking.NaiveBayesLearning._
import duckling.ranking.Testing.Corpus
import duckling.JsonSerde._
import duckling.dimension.CorpusSets
import duckling.dimension.CorpusSets.CorpusSet
import duckling.types.Node

object NaiveBayesRank extends LazyLogging {

  private val dimPath = "model.bayes.dims"
  private val modelPath = "model.bayes.file"

  private val dims =
    if (conf.hasPath(dimPath)) {
      conf.getStringList(dimPath).asScala.map(s => CorpusSets.namedDimensions(s.toLowerCase)).toList
    } else {
      throw new IllegalArgumentException("no dimension specified, ignore")
    }

  // naive bayes支持的dimension
  private val namedCorpus: List[CorpusSet] = dims.map { dim =>
    (dim, dim.corpus, Rules.rulesFor(null, Set(dim)))
  }

  private val rules: List[Rule] =
    namedCorpus.flatMap(_._3).groupBy(_.name).values.map(_.head).toList

  private val classifiers: Classifiers = {
    try {
      val path =
        if (conf.hasPathOrNull(modelPath)) conf.getString(modelPath)
        else "file_not_found"
      logger.info("read NaiveBayes model from resource")
      Resources.reader(path)(in => read[Classifiers](in))
    } catch {
      case _: Throwable =>
        logger.warn("model not found, training now")
        makeClassifiers(
          rules,
          namedCorpus
            .map { case (_, corpus: Corpus, _) => corpus }
            .reduce((a, b) => (a._1, a._2, a._3 ++ b._3))
        )
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
          case None => 0.0
          case Some(c) =>
            val feats = extractFeatures(node)
            val childSum = children.map(score).sum
            if (feats.nonEmpty) Bayes.classify(c, feats)._2 + childSum
            else childSum
        }
    }
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 0) {
      val file = Paths.get(args(0))
      val writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)
      writePretty(classifiers, writer)
      writer.close()

      val reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)
      read[Classifiers](reader)
    }
  }
}
