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

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.time.Types.{InstantValue, SimpleValue, IntervalValue}
import com.xiaomi.duckling.dimension.time.enums.Grain.Year
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.ReplacePartPredicate
import com.xiaomi.duckling.ranking.Testing
import com.xiaomi.duckling.ranking.Testing._
import com.xiaomi.duckling.UnitSpec
import com.xiaomi.duckling.dimension.time.duration.Duration

class TimeTest extends UnitSpec {

  private val options = testOptions.copy(targets = Set(Time))
  private val combinationOptions = {
    val rankOptions = new RankOptions()
    rankOptions.setCombinationRank(true)
    options.copy(rankOptions = rankOptions, full = false)
  }

  describe("single case") {
    val opt = options.copy(full = false)

    def parse(sentence: String, options: Options = opt, context: Context = testContext) = {
      analyze(sentence, context, options)
    }

    it("零一年五月八月") {
      val a = parse("零一年五月八月", combinationOptions)
      a.head.token.value match {
        case TimeValue(_, _, _, Some(x), _, _) => x shouldBe ("2001-05-xTx:x:x", false)
        case _ => true shouldBe false
      }
    }

    it("八点九点") {
      parse("八点九点").map { a =>
        a.token.value match {
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _, _) =>
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
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _, _) =>
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
          case TimeValue(SimpleValue(InstantValue(datetime, grain)), _, _, _, _, _) =>
            datetime.year
          case _ => 0
        }
      }.headOption should contain (2021)
    }

    it("不支持日到月等") {
      val cases = Table("query", "八号到十二月", "八点到五分", "春天一点", "这十分钟后", "十后", "后三十分钟后", "早上晚上八点")
      forAll(cases) { query =>
        parse(query, context = testContext, options = options) should have size 0
      }
    }

    it("beforeEndOfInterval") {
      val _options = options.copy(timeOptions = new TimeOptions)
      _options.timeOptions.setBeforeEndOfInterval(true)

      val cases = Table(("query", "start", "end")
        , ("三点到五点", "12 03:00:00", "12 05:00:00")
        , ("星期一到星期三", "2013-02-11", "2013-02-14")
        , ("1月到三月", "2013-01-01", "2013-04-01")
        , ("凌晨", "12 00:00:00", "12 06:00:00")
        , ("过年到现在", "农历 二〇一三年正月初一", "2013-02-12")
        , ("一月八号到过年", "2014-01-08", "农历 二〇一四年正月初二")
        , ("明年一月五号到二月二十号", "2014-01-05", "2014-02-21")
      )

      forAll(cases) { (query, s, t) =>
        val answers = parse(query, options = _options)
        answers should not be empty
        answers.head.token.value match {
          case TimeValue(IntervalValue(InstantValue(start, _), InstantValue(end, _)), _, _, _, _, _) =>
            start.toString should include(s)
            end.toString should include(t)
          case _ => "parsing failure"
        }
      }
    }

    it("不需要支持的") {
      val opt = options.copy(full = true, withLatent = false)
      val cases = Table("query",
        // fix #187
        "国庆晚", "十月一晚", "下钟", "晚",
        // fix #259
        "上午今天早上七点")
      forAll(cases) {query =>
        parse(query, options = opt) should be (empty)
      }
      parse("六点半", options = opt.copy(targets = Set(Duration))) should be (empty)
    }
  }

  describe("TimeSimpleTest") {
    val testCases = List(
      ("2021年", ("2021-x-xTx:x:x", false)),
      ("三月", ("x-03-xTx:x:x", false)),
      ("10号", ("x-x-10Tx:x:x", false)),
      ("12点", ("x-x-xT12:x:x", false)),
      ("12点三分", ("x-x-xT12:03:x", false)),
      ("今天十二点三十分", ("x-x-xT12:30:x", true)),
      ("10号十二点三十分", ("x-x-10T12:30:x", false)),
      ("10月九号八点三十分", ("x-10-09T08:30:x", false)),
      ("今年国庆节", ("x-10-01Tx:x:x", true)),
      ("今年中秋节", ("x-08-15Tx:x:x", true)),
      ("2021年除夕", ("2021-x-xTx:x:x", false)),
      ("2021年清明", ("2021-x-xTx:x:x", false)),
      ("2021年国庆节", ("2021-10-01Tx:x:x", false)),
      ("2021年中秋节", ("2021-08-15Tx:x:x", false)),
      ("2021年除夕下午六点三十分", ("2021-x-xT18:30:x", false)),
      ("2021年清明上午九点三十分", ("2021-x-xT09:30:x", false)),
      ("2021年国庆节六点三十分", ("2021-10-01T06:30:x", false)),
      ("2021年中秋节晚上八点三十分", ("2021-08-15T20:30:x", false)),
      ("今年三月十二", ("x-03-12Tx:x:x", true)),
      ("三月十二", ("x-03-12Tx:x:x", false)),
      ("2021年三月十二", ("2021-03-12Tx:x:x", false)),
      ("2021年三月十二下午三点十八分", ("2021-03-12T15:18:x", false))
    )

    it("simple eq") {
      testCases.foreach {
        case (query, target) =>
          val answers = analyze(query, testContext, options)
          answers.head.token.value match {
            case TimeValue(_, _, _, x, _, _) => x shouldBe Some(target)
            case _ => true shouldBe false
          }
      }
    }
  }
}
