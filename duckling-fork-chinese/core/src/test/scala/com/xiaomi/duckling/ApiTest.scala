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

package com.xiaomi.duckling

import com.xiaomi.duckling.dimension.age.Age
import com.xiaomi.duckling.dimension.currency.Currency
import com.xiaomi.duckling.dimension.episode.Episode
import com.xiaomi.duckling.dimension.level.Level
import com.xiaomi.duckling.dimension.numeral.fraction.{Fraction, FractionData}
import org.scalatest.{FunSpec, Matchers}
import com.xiaomi.duckling.dimension.numeral.{DoubleSideIntervalValue, Numeral, NumeralValue, OpenIntervalValue}
import com.xiaomi.duckling.dimension.ordinal.{Ordinal, OrdinalData}
import com.xiaomi.duckling.dimension.quantity.QuantityValue
import com.xiaomi.duckling.dimension.rating.Rating
import com.xiaomi.duckling.dimension.season.Season
import com.xiaomi.duckling.dimension.temperature.Temperature
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.dimension.time.duration.Duration
import com.xiaomi.duckling.dimension.time.date.Date


class ApiTest extends FunSpec with Matchers {

  import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

  describe("ApiTest") {

    it("should analyze") {
      val options = testOptions.copy(targets = Set(Numeral), full = false)
      println(Api.analyze("三百四十九和五十四", testContext, options))
    }

    it("should date") {
      val options = testOptions.copy(targets = Set(Numeral, Time, Duration, Date), full = false)
      println(Api.analyze("1小时05分", testContext, options))
    }

    it("should age") {
      val options = testOptions.copy(targets = Set(Age), full = false)
      val queries = List("小明三岁半", "六岁以上", "一到三岁半")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: DoubleSideIntervalValue  => println(data.schema())
                case data: NumeralValue  => println(data.schema())
                case data: OpenIntervalValue  => println(data.schema())
                case _ => println("error")
              }
          }
      }
    }

    it("should currency") {
      val options = testOptions.copy(targets = Set(Currency), full = false)
      val queries = List("九十九元九角九分", "九分钱", "九毛钱", "九元零九分", "九毛九")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: QuantityValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }
    it("should Episode") {
      val options = testOptions.copy(targets = Set(Episode), full = false)
      val queries = List("倒数第一集", "第三期", "第一百一十一期")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: QuantityValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should Season") {
      val options = testOptions.copy(targets = Set(Season), full = false)
      val queries = List("倒数第2季", "第三卷", "最后一部")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: QuantityValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should level") {
      val options = testOptions.copy(targets = Set(Level), full = false)
      val queries = List("第五档", "三十3级", "五点二级")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: NumeralValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should rating") {
      val options = testOptions.copy(targets = Set(Rating), full = false)
      val queries = List("评分8点5分", "5分以上", "评分九点零以下", "评分在7到8.5分之间")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: DoubleSideIntervalValue  => println(data.schema)
                case data: NumeralValue  => println(data.schema)
                case data: OpenIntervalValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should Temperature") {
      val options = testOptions.copy(targets = Set(Temperature), full = false)
      val queries = List("摄氏30度", "零下25.3摄氏度", "2十一华氏度", "华氏22点6度", "负的13华氏度")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: QuantityValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should ordinal") {
      val options = testOptions.copy(targets = Set(Ordinal), full = false)
      val queries = List("第三个", "第五集", "倒数第二", "第三点五")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: OrdinalData  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should Numeral") {
      val options = testOptions.copy(targets = Set(Numeral), full = false)
      val queries = List("九十九", "三点六", "负五十")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: NumeralValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }

    it("should fraction") {
      val options = testOptions.copy(targets = Set(Fraction), full = false)
      val queries = List("百分之六十", "一百二十八分之64", "一半", "负三分之二", "3/4")
      queries.foreach{
        query =>
          Api.analyze(query, testContext, options).foreach{
            answer =>
              answer.token.value match {
                case data: NumeralValue  => println(data.schema)
                case _ => println("error")
              }
          }
      }
    }
  }
}

