package com.xiaomi.duckling.dimension.time.helper

import java.time.{LocalDateTime, LocalTime}

import com.github.heqiao2010.lunar.LunarCalendar

import com.xiaomi.duckling.dimension.time.TimeValue
import com.xiaomi.duckling.dimension.time.helper.TimeObjectHelpers.timeValue
import com.xiaomi.duckling.Types.{Context, ZoneCN}
import com.xiaomi.duckling.dimension.time.enums._
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.ranking.Testing.testContext

object TimeValueHelpers {
  def datetime(d: DuckDateTime, g: Grain, holiday: Option[String]): TimeValue = {
    TimeValue(SimpleValue(InstantValue(d, g)), holiday = holiday)
  }

  def datetime(d: LocalDateTime, g: Grain, holiday: Option[String] = None): TimeValue = {
    TimeValue(SimpleValue(InstantValue(new DuckDateTime(d), g)), holiday = holiday)
  }

  def h(hour: Int) = {
    datetime(LocalDateTime.of(2013, 2, 12, hour, 0, 0), Hour)
  }

  def hm(hour: Int, minute: Int) = {
    datetime(LocalDateTime.of(2013, 2, 12, hour, minute, 0), Minute)
  }

  def hms(hour: Int, minute: Int, second: Int) = {
    datetime(LocalDateTime.of(2013, 2, 12, hour, minute, second), Second)
  }

  def ymd(y: Int = 2013,
          m: Int = 2,
          d: Int = 12,
          grain: Grain = Day,
          holiday: Option[String] = None,
          calendar: Calendar = Solar,
          isLeapMonth: Boolean = false,
          direction: Option[IntervalDirection] = None): TimeValue = {
    if (calendar == Solar) {
      if (direction.isEmpty) {
        datetime(LocalDateTime.of(y, m, d, 0, 0, 0), grain).copy(holiday = holiday)
      } else {
        val tv = datetime(LocalDateTime.of(y, m, d, 0, 0, 0), grain).copy(holiday = holiday)
        TimeValue(OpenIntervalValue(tv.timeValue.asInstanceOf[SimpleValue].instant, direction.get))
      }
    } else {
      val dt =
        DuckDateTime(LunarDate(new LunarCalendar(y, m, d, isLeapMonth)), LocalTime.of(0, 0), ZoneCN)
      datetime(dt, grain, holiday)
    }
  }

  def md(m: Int, d: Int, holiday: Option[String] = None): TimeValue = {
    datetime(LocalDateTime.of(2013, m, d, 0, 0, 0), Day).copy(holiday = holiday)
  }

  def y(y: Int): TimeValue = {
    datetime(LocalDateTime.of(y, 1, 1, 0, 0, 0), Year)
  }

  def ym(y: Int, m: Int): TimeValue = {
    datetime(LocalDateTime.of(y, m, 1, 0, 0, 0), Month)
  }

  def m(m: Int): TimeValue = ym(2013, m)

  def md(m: Int, d: Int): TimeValue = ymd(2013, m, d)

  def yMdHms(y: Int = 2013, M: Int = 2, d: Int = 12, H: Int = 0, m: Int = 0, s: Int = 0, grain: Grain): TimeValue = {
    datetime(LocalDateTime.of(y, M, d, H, m, s), grain)
  }

  def datetimeInterval(dt1: DuckDateTime, dt2: DuckDateTime, g: Grain, holiday: Option[String] = None,
                       partOfDay: Option[String] = None): TimeValue = {
    val v = timeValue(TimeObject(dt1, g, Some(dt2)))
    TimeValue(v, holiday = holiday, partOfDay = partOfDay)
  }

  def localDateTimeInterval(d1: LocalDateTime,
                            d2: LocalDateTime,
                            g: Grain,
                            holiday: Option[String] = None,
                            partOfDay: Option[String] = None): TimeValue = {
    datetimeIntervalHolidayHelper(d1, Some(d2), g, holiday, testContext).copy(partOfDay = partOfDay)
  }

  def lunarDateTimeInterval(d1: LunarCalendar,
                            t1: LocalTime,
                            d2: LunarCalendar,
                            t2: LocalTime,
                            g: Grain,
                            holiday: Option[String] = None,
                            partOfDay: Option[String] = None): TimeValue = {
    datetimeIntervalHolidayHelper(
      DuckDateTime(LunarDate(d1), t1, ZoneCN),
      Some(DuckDateTime(LunarDate(d2), t2, ZoneCN)),
      g,
      holiday,
      testContext
    ).copy(partOfDay = partOfDay)
  }

  def datetimeIntervalHolidayHelper(d1: DuckDateTime,
                                    md2: Option[DuckDateTime],
                                    g: Grain,
                                    holiday: Option[String],
                                    context: Context): TimeValue = {
    val v = timeValue(TimeObject(d1, g, md2))
    TimeValue(v, holiday = holiday)
  }

  def datetimeIntervalHolidayHelper(d1: LocalDateTime,
                                    md2: Option[LocalDateTime],
                                    g: Grain,
                                    holiday: Option[String],
                                    context: Context): TimeValue = {
    datetimeIntervalHolidayHelper(
      new DuckDateTime(d1),
      md2.map(new DuckDateTime(_)),
      g,
      holiday,
      context
    )
  }
}
