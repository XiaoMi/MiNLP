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

import org.apache.commons.lang3.time.StopWatch
import org.json4s.jackson.Serialization.write

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Api.formatToken
import com.xiaomi.duckling.JsonSerde._
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.RuleSets
import com.xiaomi.duckling.ranking.Ranker
import com.xiaomi.duckling.ranking.Testing.testContext

object NaiveBayesDebug {
  private val context = testContext // .copy(referenceTime = ZonedDateTime.now())

  // 方便设置跳过训练的条件断点
  var debug = false

  def show(answer: Answer) = {
    val entity = formatToken(answer.sentence, withNode = true)(answer.token)
    val json = write(answer.token.value)
    answer.token.value.schema match {
      case Some(schema) => println("%.5f => %s\n%s".format(answer.score, schema, json))
      case None => println("%.5f => %s".format(answer.score, json))
    }

    NaiveBayesConsole.ptree(answer.sentence)(entity)
  }

  def main(args: Array[String]): Unit = {
    val Array(dim, sentence) = args
    val targets = dim.split(",").map(s => RuleSets.namedDimensions(s.toLowerCase())).toSet
    val options = Options(targets = targets, withLatent = false, full = true)
    options.rankOptions.setRanker(Some(Ranker.NaiveBayes))
    options.rankOptions.setWinnerOnly(true)
    options.rankOptions.setCombinationRank(false)
    options.rankOptions.setRangeRankAhead(false)
    options.timeOptions.setResetTimeOfDay(false)
    options.timeOptions.setRecentInFuture(true)
    options.timeOptions.setAlwaysInFuture(true)
    options.numeralOptions.setAllowZeroLeadingDigits(false)
    options.numeralOptions.setCnSequenceAsNumber(false)

    // 初始化分类器
    if (options.rankOptions.ranker.nonEmpty) Api.analyze("今天123", context, options)

    debug = true

    val watch = new StopWatch()
    watch.start()
    val answers = Api.analyze(sentence, context, options)
    watch.stop()
    print(s"cost: ${watch.getTime()} ms, ")
    if (answers.isEmpty) println("empty results")
    else println(s"found ${answers.size} results")

    answers.foreach(show)
  }
}
