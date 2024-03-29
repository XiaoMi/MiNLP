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

import java.time.ZonedDateTime
import java.util.Locale

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Types.{Context, Options}
import com.xiaomi.duckling.dimension.time.{Examples, Time}
import com.xiaomi.duckling.dimension.time.duration.Duration

object TimeAnswerSizeDetector {
  def main(args: Array[String]): Unit = {
    val options =
      Options(withLatent = false, full = true, targets = Set(Time, Duration))
    val context = Context(ZonedDateTime.now(), Locale.CHINA)

    val examples = Examples.allExamples
    val suspicious = examples
      .map {
        case (doc, _) =>
          Api.analyze(doc.rawInput, context, options)
      }
      .filter(_.size > 1)

    println("found %d suspicious".format(suspicious.size))
    suspicious.foreach(as => println(as.head.sentence))
  }
}
