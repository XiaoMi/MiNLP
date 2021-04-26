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

import java.time.{LocalDateTime, ZonedDateTime}

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.time.Types.{InstantValue, SimpleValue}
import com.xiaomi.duckling.dimension.time.enums.Grain.Year
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.ReplacePartPredicate
import com.xiaomi.duckling.ranking.Testing
import com.xiaomi.duckling.ranking.Testing._

class TimeTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

  private val options = testOptions.copy(targets = Set(Time))
  private val combinationOptions =
    options.copy(rankOptions = RankOptions(combinationRank = true), full = false)

  describe("single case") {
    val opt = options.copy(full = false)

    def parse(sentence: String, options: Options = opt, context: Context = testContext) = {
      analyze(sentence, context, options)
    }

    it("零一年五月八月") {
      val a = parse("零一年五月八月", combinationOptions)
      a.head.token.value match {
        case TimeValue(_, _, _, Some(x), _) => x shouldBe "2001-05-xTx:x:x"
        case _ => true shouldBe false
      }
    }

    it("八点九点") {
      parse("八点九点").map { a =>
        a.token.value match {
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _) =>
            datetime.hour
        }
      } should contain theSameElementsInOrderAs List(8, 9)
    }

    it("去年的今天") {

      val td1 = cycleNth(Year, -1)
      val td2 = today

      val rpp = ReplacePartPredicate(td1, td2)
      val tdDateTime = TimeData(rpp, timeGrain = td2.timeGrain)
      val tv = tdDateTime.resolve(Testing.testContext, Testing.testOptions).get._1

      tv.timeValue.asInstanceOf[SimpleValue].instant.toString should include("2012-02-12")
    }

    it("2月11 (ref 2013-2-12)") {
      parse("2月11").map { a =>
        a.token.value match {
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _) =>
            datetime.year
        }
      }.head shouldBe 2014
    }

    it("二零二零年的阴历六月一号明天是几号") {
      // sequence predicate 中2020年历法也需要是农历
      noException should be thrownBy parse("二零二零年的阴历六月一号明天是几号")
    }

    it("二零零八年八月初六到现在") {
      noException should be thrownBy parse("二零零八年八月初六到现在活了多少天")
    }

    it("节气当天的问节气应还是当天") {
      // 节气 雨水
      val dt = LocalDateTime.of(2021, 2, 18, 8, 0)
      val context = testContext.copy(referenceTime = ZonedDateTime.of(dt, ZoneCN))

      parse("雨水", context = context).map { a =>
        a.token.value match {
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _) =>
            datetime.year
          case _ => 0
        }
      }.headOption should contain (2021)
    }
  }

  describe("TimeSimpleTest") {
    val testCases = List(
      ("2021年", "2021-x-xTx:x:x"),
      ("三月", "x-03-xTx:x:x"),
      ("10号", "x-x-10Tx:x:x"),
      ("12点", "x-x-xT12:x:x"),
      ("12点三分", "x-x-xT12:03:x"),
      ("今天十二点三十分", "x-x-xT12:30:x"),
      ("10号十二点三十分", "x-x-10T12:30:x"),
      ("10月九号八点三十分", "x-10-09T08:30:x"),
      ("今年国庆节", "x-10-01Tx:x:x"),
      ("今年中秋节", "x-08-15Tx:x:x"),
      ("2021年除夕", "2021-x-xTx:x:x"),
      ("2021年清明", "2021-x-xTx:x:x"),
      ("2021年国庆节", "2021-10-01Tx:x:x"),
      ("2021年中秋节", "2021-08-15Tx:x:x"),
      ("2021年除夕下午六点三十分", "2021-x-xT18:30:x"),
      ("2021年清明上午九点三十分", "2021-x-xT09:30:x"),
      ("2021年国庆节六点三十分", "2021-10-01T06:30:x"),
      ("2021年中秋节晚上八点三十分", "2021-08-15T20:30:x"),
      ("今年三月十二", "x-03-12Tx:x:x"),
      ("三月十二", "x-03-12Tx:x:x"),
      ("2021年三月十二", "2021-03-12Tx:x:x"),
      ("2021年三月十二下午三点十八分", "2021-03-12T15:18:x")
    )

    it("simple eq") {
      testCases.foreach {
        case (query, target) =>
          val answers = analyze(query, testContext, options)
          answers.head.token.value match {
            case TimeValue(_, _, _, x, _) => x shouldBe Some(target)
            case _ => true shouldBe false
          }
      }
    }
  }
}
