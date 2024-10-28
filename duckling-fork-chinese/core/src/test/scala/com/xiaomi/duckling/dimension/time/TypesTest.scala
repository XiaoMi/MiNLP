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

import java.time.{LocalDate, LocalTime, ZonedDateTime}

import com.github.heqiao2010.lunar.LunarCalendar

import com.xiaomi.duckling.dimension.time.enums.Grain.Day
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.helper.TimeObjectHelpers.{timeIntersect, timePlus}
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.ranking.Testing
import com.xiaomi.duckling.Types.{Options, ZoneCN}
import com.xiaomi.duckling.UnitSpec

class TypesTest extends UnitSpec {

  describe("TypesTest") {

    def round1(refTime: TimeObject, td: TimeData, options: Options): Option[TimeObject] = {
      val tc = TimeContext(
        refTime = refTime,
        maxTime = timePlus(refTime, Grain.Year, 2000),
        minTime = timePlus(refTime, Grain.Year, -2000)
      )
      val (past, future) = runPredicate(td.timePred)(refTime, tc, options)

      val valueOpt = future match {
        case Stream.Empty => past.headOption
        case ahead #:: nextAhead #:: _
            if td.notImmediate && timeIntersect(ahead)(refTime).nonEmpty =>
          Some(nextAhead)
        case ahead #:: _ => Some(ahead)
      }
      valueOpt
    }

    it("sequence apply demo") {
      val refTime = new TimeObject(Testing.testContext.referenceTime, Grain.Second)
      val options = Options()
      val td1 = cycleNth(Day, 1)

      val r1 = round1(refTime, td1, options).get
      r1.start.dayOfMonth shouldBe 13

      val td2 = cycleNth(Day, 2)
      val r2 = round1(r1, td2, options).get
      r2.start.dayOfMonth shouldBe 15
    }

    it("DuckDateTime 公历 isBefore") {
      val a = DuckDateTime(SolarDate(LocalDate.of(2013, 3, 1)), LocalTime.of(0, 0), ZoneCN)
      val b = DuckDateTime(SolarDate(LocalDate.of(2013, 2, 28)), LocalTime.of(0, 0), ZoneCN)

      b.isBefore(a) shouldBe true
      a.isAfter(b) shouldBe true
    }

    it("DuckDateTime 农历 isBefore") {
      val a = DuckDateTime(LunarDate(new LunarCalendar(2013, 3, 1, false)), LocalTime.of(0, 0), ZoneCN)
      val b = DuckDateTime(LunarDate(new LunarCalendar(2013, 2, 28, false)), LocalTime.of(0, 0), ZoneCN)

      b.isBefore(a) shouldBe true
      a.isAfter(b) shouldBe true
    }

    it("DuckDateTime to ZonedDateTime") {
      val da = DuckDateTime(LunarDate(new LunarCalendar(2013, 3, 1, false)), LocalTime.of(0, 0), ZoneCN)
      val za = ZonedDateTime.of(2013, 4, 10, 0, 0, 0, 0, ZoneCN)

      val db = DuckDateTime(SolarDate(LocalDate.of(2013, 2, 28)), LocalTime.of(0, 0), ZoneCN)
      val zb = ZonedDateTime.of(2013, 2, 28, 0, 0, 0, 0, ZoneCN)

      da.toZonedDateTime shouldBe za
      db.toZonedDateTime shouldBe zb
    }
  }
}
