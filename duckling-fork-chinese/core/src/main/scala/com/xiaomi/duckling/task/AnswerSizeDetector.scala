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

import org.json4s.jackson.Serialization.write

import java.time.ZonedDateTime
import java.util.Locale

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Api.formatToken
import com.xiaomi.duckling.JsonSerde._
import com.xiaomi.duckling.Types.{Context, Options, RankOptions}
import com.xiaomi.duckling.dimension.CorpusSets

object AnswerSizeDetector {
  def main(args: Array[String]): Unit = {
    val Array(dims, query) = args
    val targets = dims.split(",").map(CorpusSets.namedDimensions).toSet

    val options = Options(withLatent = false, full = false,
      rankOptions = RankOptions(winnerOnly = false), targets = targets)
    val context = Context(ZonedDateTime.now(), Locale.CHINA)

    Api.analyze("123", context, options)

    val start = System.currentTimeMillis()
    val answers = Api.analyze(query, context, options)
    val end = System.currentTimeMillis()
    answers.foreach { a =>
      val entity = formatToken(a.sentence, withNode = true)(a.token)
      println("%.5f => %s".format(a.score, write(a.token.value)))
      NaiveBayesConsole.ptree(a.sentence)(entity)
    }
    println(s"$query => |${answers.length}|, cost = ${end - start}")
  }
}
