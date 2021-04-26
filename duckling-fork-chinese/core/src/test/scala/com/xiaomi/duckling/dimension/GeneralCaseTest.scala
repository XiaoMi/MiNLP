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

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

class GeneralCaseTest
    extends FunSpec
    with Matchers
    with TableDrivenPropertyChecks
    with LazyLogging {

  val dims =
    if (sys.props.contains("duckling.dim")) {
      val list = sys.props("duckling.dim").split(",").map(CorpusSets.namedDimensions).toList
      logger.info(s"test only dims of: ${list.map(_.name).mkString("[", ", ", "]")}")
      list
    } else {
      logger.info("test all dims")
      CorpusSets.dims
    }.distinct

  val naiveBayesDims = dims.distinct

  describe("General cases: naive bayes") {
    for (dim <- naiveBayesDims) {
      val corpusTable = Table(s"${dim.name} - examples", dim.allExamples: _*)

      val options = testOptions.copy(targets = Set(dim), debug = true)

      it(s"${dim.name} - cases") {
        forEvery(corpusTable) {
          case (doc, predicate, _) =>
            val candidates = analyze(doc.rawInput, testContext, options)
            val rs =
              candidates.zipWithIndex.find {
                case (c, id) =>
                  predicate(doc, testContext)(c.token)
              } match {
                case Some((_, 0)) => logger.info(s"✅ ${doc.rawInput}"); true
                case Some((_, i)) =>
                  logger.error(s"️❌ ${doc.rawInput} - expected answer is at [$i], be care"); false
                case None =>
                  logger.error(s"❌ ${doc.rawInput} - build error, composing failure"); false
              }
            rs shouldBe true
        }
      }
    }
  }
}
