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

package com.xiaomi.duckling.task

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

import org.fusesource.jansi.Ansi
import org.jline.reader.{EndOfFileException, LineReader, LineReaderBuilder}
import org.jline.reader.impl.completer._
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.json4s.jackson.Serialization.write

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Api.formatToken
import com.xiaomi.duckling.JsonSerde._
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher._
import com.xiaomi.duckling.dimension.FullDimensions
import com.xiaomi.duckling.ranking.Ranker
import com.xiaomi.duckling.ranking.Testing.testContext
import com.xiaomi.duckling.types.Node

/**
 * sbt duckConsole的jline启动有点问题，可以使用sbt console
 *
 * @example <p>sbt core/console
 *          <p>> com.xiaomi.duckling.task.NaiveBayesConsole.run()
 *          <p>> dimension time duration
 *          <p>> 今天的天气
 *          <p>> option with-latent false
 *          <p>...
 */
object NaiveBayesConsole extends LazyLogging {
  private val context = testContext.copy(referenceTime = ZonedDateTime.now(ZoneCN))

  // 方便设置训练捂的断点
  var debug = false

  def buildLineReader(): LineReader = {
    val terminal = TerminalBuilder
      .builder()
      .encoding(StandardCharsets.UTF_8)
      .name("DUCK")
      .build();

    val dimension = new ArgumentCompleter(
      new StringsCompleter("dimension"),
      new StringsCompleter(FullDimensions.namedDimensions.keys.toList: _*)
    )

    val options = new ArgumentCompleter(
      new StringsCompleter("option"),
      new StringsCompleter(
        "winner-only",
        "with-latent",
        "full",
        "inherit-duration-grain",
        "seasons",
        "sequence",
        "fuzzy-on",
        "before-end-of-interval",
        "recent-in-future",
        "always-in-future"
      ),
      NullCompleter.INSTANCE
    )

    val completer = new AggregateCompleter(dimension, options)

    LineReaderBuilder
      .builder()
      .appName("duckling - console")
      .terminal(terminal)
      .parser(new DefaultParser())
      .completer(completer)
      .build()
  }

  def getPrompt(): String = {
    Ansi
      .ansi()
      .eraseScreen()
      .fg(Ansi.Color.BLUE)
      .bold()
      .a("duckling")
      .fgBright(Ansi.Color.BLACK)
      .bold()
      .a(" > ")
      .reset()
      .toString
  }

  def setOptions(options: Options, line: String): (Boolean, Options) = {
    val cols = line.split("\\s+")
    if (cols.length >= 2) {
      if (line.startsWith("dimension ")) {
        val targets = FullDimensions.convert(cols.tail)
        (true, options.copy(targets = targets))
      } else if (line.startsWith("option ")) {
        val opt =
          if (cols.length >= 3 && Set("true", "false")
            .contains(cols(2).toLowerCase)) {
            val value = cols(2).toBoolean
            val opt = cols(1) match {
              case "winner-only" => options.rankOptions.setWinnerOnly(value); options
              case "with-latent" => options.copy(withLatent = value)
              case "full" => options.copy(full = value)
              case "inherit-duration-grain" => options.timeOptions.setInheritGrainOfDuration(value); options
              case "seasons" => options.timeOptions.setParseFourSeasons(value); options
              case "sequence" => options.timeOptions.setSequence(value); options
              case "fuzzy-on" => options.timeOptions.setDurationFuzzyOn(value); options
              case "before-end-of-interval" => options.timeOptions.setBeforeEndOfInterval(value); options
              case "recent-in-future" => options.timeOptions.setRecentInFuture(value); options
              case "always-in-future" => options.timeOptions.setAlwaysInFuture(value); options
              case _ => options
            }
            opt
          } else {
            logger.info("{}: true/false expected", cols(1))
            options
          }
        (true, opt)
      } else (false, options)
    } else (false, options)
  }

  def round(reader: LineReader, options: Options): Options = {
    reader.getTerminal.flush()

    val line =
      try {
        reader.readLine(getPrompt()).trim
      } catch {
        case ex: EndOfFileException =>
          logger.info("bye!")
          System.exit(0)
          ""
      }

    val (isOpt, _options) = setOptions(options, line)

    if (!isOpt) {
      val answers = Api.analyze(line, context, _options)

      if (answers.isEmpty) println("empty results")
      else println(s"found ${answers.size} results")

      answers.foreach { answer: Answer =>
        val entity = formatToken(line, withNode = true)(answer.token)
        val json = write(answer.token.value)
        answer.token.value.schema match {
          case Some(schema) => println("%.5f => %s\n%s".format(answer.score, schema, json))
          case None => println("%.5f => %s".format(answer.score, json))
        }
        ptree(line)(entity)
      }
    }

    _options
  }

  def run(): Unit = {
    var options = Options(targets = Set(), withLatent = false)
    options.rankOptions.setRanker(Ranker.NaiveBayes)
    options.rankOptions.setWinnerOnly(true)
    options.rankOptions.setCombinationRank(false)

    options.timeOptions.setResetTimeOfDay(false)
    options.timeOptions.setRecentInFuture(true)

    options.numeralOptions.setAllowZeroLeadingDigits(false)
    options.numeralOptions.setCnSequenceAsNumber(false)

    // 初始化分类器
    Api.analyze("今天123", context, options)

    debug = true

    val reader = buildLineReader()
    while (true) {
      options = round(reader, options)
    }
  }

  def main(args: Array[String]): Unit = {
    run()
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
