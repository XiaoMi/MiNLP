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

import java.time.{LocalDate, LocalDateTime, LocalTime, ZonedDateTime}

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.predicates.TimeDatePredicate
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.Types.{Options, ZoneCN}
import com.xiaomi.duckling.UnitSpec

class TimeDataTest extends UnitSpec with LazyLogging {

  describe("TimeDataTest") {

    it("should resolve") {
      val td = TimeData(TimeDatePredicate(year = 2018, month = 5), timeGrain = Grain.Month)
      td.resolve(testContext, testOptions.copy(targets = Set(Time))).get._1 match {
        case TimeValue(SimpleValue(InstantValue(dt, grain)), _, _, Some(simple), _, _) =>
          dt.year shouldBe 2018
          dt.month shouldBe 5
          grain shouldBe Grain.Month
          simple shouldBe ("2018-05-xTx:x:x", false)
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
      val timeOptions = new TimeOptions()
      timeOptions.setAlwaysInFuture(false)
      val options = testOptions.copy(targets = Set(Time), timeOptions = timeOptions)

      testCases.foreach {
        case (query, yyyy, mm, dd) =>
          val answers = analyze(query, testContext, options)
          answers.headOption match {
            case Some(answer) =>
              answer.token.value match {
                case TimeValue(SimpleValue(InstantValue(dt, _)), _, _, _, _, _) =>
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

    it("Grain of duration inheritance") {
      val options = testOptions.copy(targets = Set(Time))

      def parse(query: String, options: Options): InstantValue = {
        val ref = ZonedDateTime.of(LocalDate.of(2022, 10, 1), LocalTime.of(0, 0, 5), ZoneCN)
        analyze(query, testContext.copy(referenceTime = ref), options).get.head.token.value
          .asInstanceOf[TimeValue].timeValue
          .asInstanceOf[SimpleValue].instant
      }

      options.timeOptions.setInheritGrainOfDuration(false)
      val t1 = parse("三分钟后", options)
      t1.grain should (be(Grain.NoGrain) or be(Grain.Second))
      t1.datetime.toString should be ("2022-10-01 00:03:05 [+08:00]")


      options.timeOptions.setInheritGrainOfDuration(true)
      val t2 = parse("三分钟后", options)
      t2.grain shouldBe Grain.Minute
      t2.datetime.toString should be ("2022-10-01 00:03:00 [+08:00]")
    }

    it("hour/minute always in future: 2013/02/12 4:30") {
      val options = testOptions.copy(targets = Set(Time))

      def parse(query: String): String = {
        analyze(query, testContext, options).get.head.token.value
          .asInstanceOf[TimeValue].timeValue
          .asInstanceOf[SimpleValue].instant
          .datetime.toLocalDatetime
          .toString
      }

      parse("4点") shouldBe "2013-02-12T16:00"
      parse("12号") shouldBe "2013-02-12T00:00"
      parse("2月") shouldBe "2013-02-01T00:00"
    }
  }
}
