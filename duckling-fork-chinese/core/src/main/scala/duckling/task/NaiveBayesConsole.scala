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

package duckling.task

import java.time.ZonedDateTime

import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.json4s.jackson.Serialization.write

import duckling.Api
import duckling.Api.formatToken
import duckling.JsonSerde._
import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.CorpusSets
import duckling.dimension.matcher._
import duckling.dimension.numeral.NumeralOptions
import duckling.dimension.time.TimeOptions
import duckling.ranking.{Ranker, Testing}
import duckling.types.Node

object NaiveBayesConsole {
  private val context = Testing.testContext.copy(referenceTime = ZonedDateTime.now())

  // 方便设置训练捂的断点
  var debug = false

  def main(args: Array[String]): Unit = {
    val dim = args(0)
    val targets = dim.split(",").map(s => CorpusSets.namedDimensions(s.toLowerCase())).toSet
    val options = Options(
      targets = targets,
      withLatent = false,
      rankOptions =
        RankOptions(ranker = Ranker.NaiveBayes, winnerOnly = true, combinationRank = true),
      timeOptions = TimeOptions(resetTimeOfDay = true, recentInFuture = false),
      numeralOptions = NumeralOptions(allowZeroLeadingDigits = false, cnSequenceAsNumber = false)
    )

    // 初始化分类器
    Api.analyze("今天123", context, options)

    debug = true

    val terminal = TerminalBuilder.builder.build()

    val reader = LineReaderBuilder
      .builder()
      .appName("duckling - example")
      .terminal(terminal)
      .build()

    while (true) {
      val line = reader.readLine("input > ").trim
      terminal.flush()
      val answers = Api.analyze(line, context, options)

      if (answers.isEmpty) println("empty results")
      else println(s"found ${answers.size} results")

      answers.foreach { answer: Answer =>
        val entity = formatToken(line, withNode = true)(answer.token)
        println("%.5f => %s".format(answer.score, write(answer.token.value)))
        ptree(line)(entity)
      }
    }
  }

  def pnode(sentence: String, depth: Int)(node: Node): Unit = {
    val name = node.token.dim match {
      case RegexMatch     => "regex"
      case VarcharMatch   => "varchar"
      case PhraseMatch    => "phrase"
      case MultiCharMatch => "multi-char"
      case LexiconMatch   => "lexicon"
      case _              => node.rule.get
    }
    val body = sentence.substring(node.range.start, node.range.end)
    val out = "%s%s[\"%s\"]".format("-- " * depth, name, body)
    println(out)
    System.out.flush()
    node.children.foreach(pnode(sentence, depth + 1))
  }

  def ptree(sentence: String)(entity: Entity): Unit = {
    entity.enode.foreach(pnode(sentence, 0))
  }
}
