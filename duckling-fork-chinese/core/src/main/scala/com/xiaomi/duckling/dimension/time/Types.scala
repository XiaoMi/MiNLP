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

import java.time._
import java.time.format.DateTimeFormatter
import java.util.{Calendar => JCal}

import com.github.heqiao2010.lunar.{LunarCalendar, LunarUtils}

import com.xiaomi.duckling.Types.ZoneCN
import com.xiaomi.duckling.dimension.time.enums._

object Types {

  val TimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
  val DateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  val SECONDS_OF_DAY: Int = 60 * 60 * 24
  val MINUTE_OF_DAY: Int = 60 * 24

  trait DuckDate {
    def year: Int
    def month: Int
    def dayOfMonth: Int

    def of(y: Int, m: Int, d: Int): DuckDate

    def minusDays(n: Int): DuckDate

    def dayOfWeek: Int

    /**
      * 设置年
      */
    def withYear(y: Int): DuckDate

    /**
      * 设置月
      */
    def withMonth(m: Int): DuckDate

    /**
      * 设置日
      */
    def withDayOfMonth(d: Int): DuckDate

    def plusDays(n: Int): DuckDate
    def plusWeeks(n: Int): DuckDate
    def plusMonths(n: Int): DuckDate
    def plusYears(n: Int): DuckDate

    def toLunar: LunarDate
    def toSolar: SolarDate

    /**
      * 当月的天数
      */
    def lengthOfMonth: Int

    /**
      * 与另一个日期比较，返回 -1/0/1 表示大小比较
      */
    def comparedTo(o: DuckDate): Int
  }

  case class SolarDate(date: LocalDate) extends DuckDate {
    override def year: Int = date.getYear

    override def month: Int = date.getMonthValue

    override def dayOfMonth: Int = date.getDayOfMonth

    override def dayOfWeek: Int = date.getDayOfWeek.getValue

    override def of(y: Int, m: Int, d: Int): DuckDate = this.copy(date = LocalDate.of(y, m, d))

    override def minusDays(n: Int): DuckDate = this.copy(date = date.minusDays(n))

    override def plusDays(n: Int): DuckDate = this.copy(date = date.plusDays(n))

    override def plusWeeks(n: Int): DuckDate = this.copy(date = date.plusWeeks(n))

    override def plusMonths(n: Int): DuckDate = this.copy(date = date.plusMonths(n))

    override def plusYears(n: Int): DuckDate = this.copy(date = date.plusYears(n))

    override def toLunar: LunarDate = {
      val cal = JCal.getInstance()
      cal.set(year, month - 1, dayOfMonth)
      LunarDate(LunarCalendar.solar2Lunar(cal))
    }

    override def toSolar: SolarDate = this

    /**
      * 当月的天数
      */
    override def lengthOfMonth: Int = date.getMonth.length(Year.isLeap(year))

    /**
      * 与另一个日期比较，返回 -1/0/1 表示大小比较
      */
    override def comparedTo(o: DuckDate): Int = {
      o match {
        case solar @ SolarDate(_) => date.compareTo(solar.date)
        case lunar @ LunarDate(_) => date.compareTo(lunar.toSolar.date)
      }
    }

    /**
      * 设置年
      */
    override def withYear(y: Int): DuckDate = this.copy(date = date.withYear(y))

    /**
      * 设置月
      */
    override def withMonth(m: Int): DuckDate = this.copy(date = date.withMonth(m))

    /**
      * 设置日
      */
    override def withDayOfMonth(d: Int): DuckDate = this.copy(date = date.withDayOfMonth(d))

    override def toString: String = date.format(DateFormatter)
  }

  case class LunarDate(date: LunarCalendar) extends DuckDate {
    override def year: Int = date.getLunarYear

    override def month: Int = date.getLunarMonth

    override def dayOfMonth: Int = date.getDayOfLunarMonth

    def isLeapMonth: Boolean = date.isLeapMonth

    override def of(y: Int, m: Int, d: Int): DuckDate = {
      LunarDate(new LunarCalendar(y, m, d, false))
    }

    /**
      * LunarCalendar 的实现并非是不可变的，先拷贝再改变
      */
    def copySet(n: Int)(set: LunarCalendar => Unit): LunarDate = {
      if (n == 0) this
      else {
        val _new = new LunarCalendar(year, month, dayOfMonth, isLeapMonth)
        set(_new)
        LunarDate(_new)
      }
    }

    override def minusDays(n: Int): DuckDate = {
      copySet(n)(c => c.addByLunar(JCal.DAY_OF_MONTH, -n))
    }

    override def dayOfWeek: Int = toSolar.dayOfWeek

    override def plusDays(n: Int): DuckDate = {
      copySet(n)(c => c.addByLunar(JCal.DAY_OF_MONTH, n))
    }

    override def plusWeeks(n: Int): DuckDate = {
      copySet(n)(c => c.addByLunar(JCal.DAY_OF_MONTH, n * 7))
    }

    override def plusMonths(n: Int): DuckDate = {
      copySet(n)(c => c.addByLunar(JCal.MONTH, n))
    }

    override def plusYears(n: Int): DuckDate = {
      copySet(n)(c => c.addByLunar(JCal.YEAR, n))
    }

    override def toLunar: LunarDate = this

    override def toSolar: SolarDate = {
      val solar = LunarCalendar.lunar2Solar(year, month, dayOfMonth, isLeapMonth)
      val date =
        LocalDate.of(solar.get(JCal.YEAR), solar.get(JCal.MONTH) + 1, solar.get(JCal.DAY_OF_MONTH))
      SolarDate(date)
    }

    /**
      * 当月的天数
      */
    override def lengthOfMonth: Int = {
      LunarUtils.lengthOfMonth(date.getLunarYear, date.getLunarMonth, date.isLeapMonth).intValue()
    }

    /**
      * 与另一个日期比较，返回 -1/0/1 表示大小比较
      */
    override def comparedTo(o: DuckDate): Int = {
      o match {
        case l @ LunarDate(od) =>
          if (year > l.year ||
              year == l.year && month > l.month ||
              year == l.year && month == l.month && isLeapMonth > l.isLeapMonth ||
              year == l.year && month == l.month && isLeapMonth == l.isLeapMonth && dayOfMonth > l.dayOfMonth) {
            1
          } else if (year == l.year && month == l.month && isLeapMonth == l.isLeapMonth && dayOfMonth == l.dayOfMonth) {
            0
          } else -1
        case solar @ SolarDate(date) => toSolar.comparedTo(solar)
      }
    }

    /**
      * 设置年
      */
    override def withYear(y: Int): DuckDate = {
      if (y == date.getLunarYear) this
      else {
        // 闰月状态重置
        val _date = new LunarCalendar(y, date.getLunarMonth, date.getDayOfLunarMonth, false)
        this.copy(date = _date)
      }
    }

    /**
      * 设置月
      */
    override def withMonth(m: Int): DuckDate = {
      if (m == date.getLunarMonth) this
      else {
        // 闰月状态重置
        val _date = new LunarCalendar(date.getLunarYear, m, date.getDayOfLunarMonth, false)
        this.copy(date = _date)
      }
    }

    /**
      * 设置日
      */
    override def withDayOfMonth(d: Int): DuckDate = {
      if (d == date.getDayOfLunarMonth) this
      else {
        val _date = new LunarCalendar(date.getLunarYear, date.getLunarMonth, d, date.isLeapMonth)
        this.copy(date = _date)
      }
    }

    override def toString: String = {
      "农历 %s".format(date)
    }
  }

  case class DuckDateTime(date: DuckDate, time: LocalTime, zone: ZoneId) {
    def this(dt: ZonedDateTime) = {
      this(SolarDate(dt.toLocalDate), dt.toLocalTime, dt.getZone)
    }

    def this(dt: LocalDateTime, zone: ZoneId = ZoneCN) = {
      this(dt.atZone(zone))
    }

    def year: Int = date.year

    def month: Int = date.month

    def dayOfMonth: Int = date.dayOfMonth

    def hour: Int = time.getHour

    def minute: Int = time.getMinute

    def second: Int = time.getSecond

    def minusDays(n: Int): DuckDateTime = this.copy(date = date.minusDays(n))

    def dayOfWeek: Int = date.dayOfWeek

    def plusSeconds(n: Int): DuckDateTime = {
      if (n == 0) this
      else {
        val sum = time.toSecondOfDay + n
        val (days, seconds) = step(sum, SECONDS_OF_DAY)
        this.copy(
          date = date.plusDays(days),
          time = LocalTime.of(seconds / 60 / 60, seconds / 60 % 60, seconds % 60)
        )
      }
    }

    def plusMinutes(n: Int): DuckDateTime = {
      if (n == 0) this
      else {
        val sum = time.getMinute + 60 * time.getHour + n
        val (days, minutes) = step(sum, MINUTE_OF_DAY)
        this.copy(date = date.plusDays(days), time = LocalTime.of(minutes / 60, minutes % 60, time.getSecond))
      }
    }

    def plusHours(n: Int): DuckDateTime = {
      if (n == 0) this
      else {
        val sum = time.getHour + n
        val (d, h) = step(sum, 24)
        this.copy(date = date.plusDays(d), time = time.withHour(h % 24))
      }
    }

    private def step(sum: Int, a: Int): (Int, Int) = {
      if (sum >= 0) (sum / a, sum % a)
      else if (sum % a < 0) (sum / a - 1, sum % a + a)
      else (sum / a, 0)
    }

    def plusDays(n: Int): DuckDateTime = this.copy(date = date.plusDays(n))

    def plusWeeks(n: Int): DuckDateTime = this.copy(date = date.plusWeeks(n))

    def plusMonths(n: Int): DuckDateTime = this.copy(date = date.plusMonths(n))

    def plusYears(n: Int): DuckDateTime = this.copy(date = date.plusYears(n))

    def isBefore(o: DuckDateTime): Boolean = {
      date.comparedTo(o.date) match {
        case x if x < 0 => true
        case 0          => time.isBefore(o.time)
        case _          => false
      }
    }

    def isAfter(o: DuckDateTime): Boolean = {
      date.comparedTo(o.date) match {
        case x if x < 0 => false
        case 0          => time.isAfter(o.time)
        case _          => true
      }
    }

    /**
      * 一年的最后一天：下一年的前一天
      * 公历始终是 12月31日，但是腊月不是，既有可能闰月也有可能除夕是29号
      */
    def lastDayOfYear: DuckDateTime = {
      withMonth(1).withDayOfMonth(1).minusDays(1)
    }

    def withYear(y: Int): DuckDateTime = this.copy(date = date.withYear(y))

    def withMonth(m: Int): DuckDateTime = this.copy(date = date.withMonth(m))

    def withDayOfMonth(d: Int): DuckDateTime = this.copy(date = date.withDayOfMonth(d))

    def to(calendar: Calendar): DuckDateTime = {
      calendar match {
        case Solar              => this.copy(date = date.toSolar)
        case Lunar(isLeapMonth) => this.copy(date = date.toLunar)
      }
    }

    def toZonedDateTime: ZonedDateTime = {
      ZonedDateTime.of(toLocalDatetime, zone)
    }

    def toLocalDatetime: LocalDateTime = LocalDateTime.of(date.toSolar.date, time)

    override def toString: String = {
      "%s %s [%s]".format(date, time.format(TimeFormatter), zone.getId)
    }
  }

  case class InstantValue(datetime: DuckDateTime, grain: Grain)

  trait SingleTimeValue

  case class SimpleValue(instant: InstantValue) extends SingleTimeValue

  case class IntervalValue(start: InstantValue, end: InstantValue) extends SingleTimeValue

  case class OpenIntervalValue(start: InstantValue, direction: IntervalDirection)
      extends SingleTimeValue

  case class TimeObject(start: DuckDateTime,
                        grain: Grain,
                        end: Option[DuckDateTime] = None,
                        direction: Option[IntervalDirection] = None) {
    def this(ref: ZonedDateTime, grain: Grain) = {
      this(new DuckDateTime(ref), grain)
    }

    def to(calenderOpt: Option[Calendar]): TimeObject = {
      calenderOpt match {
        case Some(calendar) => this.copy(start = start.to(calendar), end = end.map(_.to(calendar)))
        case None           => this
      }
    }
  }

  case class TimeContext(refTime: TimeObject,
                         maxTime: TimeObject,
                         minTime: TimeObject,
                         reverseTake: Boolean = false)

  def fixedTimeContext(t: TimeObject): TimeContext = {
    TimeContext(t, t, t)
  }
}
