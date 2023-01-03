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

import org.scalatest.prop.TableDrivenPropertyChecks

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.{JsonSerde, UnitSpec}
import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.ranking.CorpusSets.dimExamples
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.task.NaiveBayesDebug

class GeneralCaseTest
    extends UnitSpec
    with TableDrivenPropertyChecks
    with LazyLogging {

  val dims =
    if (sys.props.contains("duckling.dim")) {
      val list = sys.props("duckling.dim").split(",").map(RuleSets.namedDimensions).toList
      logger.info(s"test only dims of: ${list.map(_.name).mkString("[", ", ", "]")}")
      list
    } else {
      logger.info("test all dims")
      RuleSets.dims
    }.distinct


  val naiveBayesDims = dims.distinct.map(dim => (dim, dimExamples(dim)))

  describe("General cases: naive bayes") {
    for ((dim, corpus) <- naiveBayesDims) {
      val corpusTable = Table(s"${dim.name} - examples", corpus.allExamples: _*)

      val options = testOptions.copy(targets = Set(dim), debug = true)

      it(s"${dim.name} - cases") {
        forAll(corpusTable) {
          case (doc, rv) =>
            val candidates = analyze(doc.rawInput, testContext, options)
            val found = candidates.zipWithIndex.find {
              case (c, _) => JsonSerde.simpleCheck(doc, c.token, rv)
            }
            val matches = found match {
              case Some((_, 0)) => logger.info(s"✅ ${doc.rawInput}"); true
              case Some((a, i)) =>
                NaiveBayesDebug.show(a)
                logger.error(s"️❌ ${doc.rawInput} - expected answer is at [$i], be care"); false
              case None =>
                candidates.foreach(NaiveBayesDebug.show)
                logger.error(s"❌ ${doc.rawInput} - build error, composing failure"); false
            }
            matches shouldBe true
        }
      }
    }
  }
}
