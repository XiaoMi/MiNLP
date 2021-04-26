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

import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Types.{Options, RankOptions}
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.ranking.Testing

object CombinationDiff {

  val options0 = Options(withLatent = false, targets = Set(Numeral))
  val options1 = Options(
    withLatent = false,
    targets = Set(Numeral),
    rankOptions = RankOptions(combinationRank = true)
  )

  def write(writer: Writer, s: String): Unit = synchronized {
    writer.write(s + "\n")
  }

  def main(args: Array[String]): Unit = {
    val lines = Files.readAllLines(Paths.get("factoid_query.txt"), StandardCharsets.UTF_8).asScala
    val writer = Files.newBufferedWriter(Paths.get("time.diff"), StandardCharsets.UTF_8)
    lines.par.foreach { line =>
      val s0 = Api.analyze(line, Testing.testContext, options0).map(_.text).mkString("/")
      val s1 = Api.analyze(line, Testing.testContext, options1).map(_.text).mkString("/")
      if (s0 != s1) {
        write(writer, s"$line: $s0 $s1")
      }
    }
    writer.close()
  }
}
