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

package com.xiaomi.duckling.dimension.time

import org.scalatest.{FunSpec, Matchers}

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.predicates.TimeDatePredicate
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

class TimeDataTest extends FunSpec with Matchers with LazyLogging {

  describe("TimeDataTest") {

    it("should resolve") {
      val td = TimeData(TimeDatePredicate(year = 2018, month = 5), timeGrain = Grain.Month)
      td.resolve(testContext, testOptions.copy(targets = Set(Time))).get._1 match {
        case TimeValue(SimpleValue(InstantValue(dt, grain)), _, _, Some(simple), _) =>
          dt.year shouldBe 2018
          dt.month shouldBe 5
          grain shouldBe Grain.Month
          simple shouldBe "2018-05-xTx:x:x"
      }
    }

    val testCases = List(
      //2013, 2, 12, 4, 30, 0
      ("昨天", 2013, 2, 11),
      ("今天", 2013, 2, 12),
      ("明天", 2013, 2, 13),
      ("一月二号", 2013, 1, 2),
      ("二月十一号", 2013, 2, 11),
      ("明年一月二号", 2014, 1, 2),
      ("今年三月十号", 2013, 3, 10)
    )

    it("not InFuture eq") {
      val timeOptions = TimeOptions(alwaysInFuture = false)
      val options = testOptions.copy(targets = Set(Time), timeOptions = timeOptions)

      testCases.foreach {
        case (query, yyyy, mm, dd) =>
          val answers = analyze(query, testContext, options)
          answers.headOption match {
            case Some(answer) =>
              answer.token.value match {
                case TimeValue(SimpleValue(InstantValue(dt, _)), _, _, _, _) =>
                  (dt.year, dt.month, dt.dayOfMonth) shouldBe(yyyy, mm, dd)
                case v =>
                  logger.warn(s"unexpected value: $query => $v")
                  true shouldBe false
              }
            case _ =>
              logger.warn(s"empty result: $query")
              true shouldBe false
          }
      }
    }
  }
}
