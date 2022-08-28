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

import com.github.heqiao2010.lunar.LunarCalendar

import java.time.{LocalDateTime, LocalTime}

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.Types.{DuckDateTime, LunarDate}
import com.xiaomi.duckling.dimension.time.enums.{IntervalDirection, Lunar}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.helper.TimeValueHelpers._

/**
 * 参考时间是: 2013年2月12日，万年历对构造用例可能会有所帮助
 * [[https://wannianli.tianqi.com/]]
 */
trait Examples extends DimExamples {

  val days = List(
    //  有一部分在Date中已经定义了
    (ymd(2013, 2, 12), List("今天", "今日", "2.12", "02.12")),
    (ymd(2013, 2, 11), List("昨天", "昨日")),
    (ymd(2013, 2, 13), List("明天", "明日")),
    (ymd(2013, 2, 14), List("后天", "后日")),
    (ymd(2013, 2, 10), List("前天", "前日")),
    (ymd(2012, 2, 12), List("去年的今天")),
    (ymd(2010, 2, 12), List("去年的前年的今天")),
    (ymd(2013, 2, 15), List("明天的后天")),
    (ymd(2013, 2, 28), List("这个月月底")),
    (ym(2013, 2), List("这个月")),
    (ym(2013, 1), List("上月", "上个月")),
    (ym(2013, 3), List("下月", "下个月", "3月", "3月份", "三月")),
    (y(2012), List("去年", "上一年")),
    (y(2013), List("今年", "这一年")),
    (y(2014), List("明年", "下一年")),
    (ymd(2013, 12, 30, calendar = Lunar(false)).copy(holiday = "除夕"), List("今年除夕", "大年三十", "年三十")),
    (ymd(2021, 12, 29, calendar = Lunar(false), holiday = "除夕"), List("2021年除夕")), // 2021年没有大年三十
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 0, 0, 0),
        LocalDateTime.of(2013, 2, 15, 0, 0, 0),
        Day
      ),
      List("接下来三天", "后三天")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 0, 0, 0),
        LocalDateTime.of(2013, 2, 14, 0, 0, 0),
        Day
      ),
      List("今明两天")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 13, 0, 0, 0),
        LocalDateTime.of(2013, 2, 15, 0, 0, 0),
        Day
      ),
      List("明后天", "明后两天")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2012, 12, 1, 0, 0, 0),
        LocalDateTime.of(2013, 2, 1, 0, 0, 0),
        Month
      ),
      List("上两个月", "上二个月", "前两个月", "前两个月", "前二个月", "之前二个月", "往前二个月", "向前二个月")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 1, 0, 0, 0),
        LocalDateTime.of(2013, 5, 1, 0, 0, 0),
        Month
      ),
      List("接下来三个月", "后三个月")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2011, 1, 1, 0, 0, 0),
        LocalDateTime.of(2013, 1, 1, 0, 0, 0),
        Year
      ),
      List("前两年")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 1, 1, 0, 0, 0),
        LocalDateTime.of(2016, 1, 1, 0, 0, 0),
        Year
      ),
      List("下三年", "未来三年")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 13, 0, 0, 0),
        LocalDateTime.of(2013, 2, 16, 0, 0, 0),
        Day
      ),
      List("明天的之后三天", "明天的往后三天", "明天的向后三天")
    )
  )

  val times = List(
    (hms(4, 30, 0), List("现在", "此时", "此刻", "当前")),
    (hm(15, 15), List("下午三点十五", "下午3:15", "15:15", "3:15pm", "3:15p.m", "下午三点一刻", "下午的三点一刻")),
    (hm(16, 40), List("十六时四十分", "十六点四十")),
    (hm(6, 10), List("六点十分", "六点一十")),
    (hms(4, 33, 0), List("过三分钟")),
    (datetime(LocalDateTime.of(2013, 2, 14, 0, 0, 0), Hour), List("明天晚上12点", "13号晚上12点")),
    (datetime(LocalDateTime.of(2013, 2, 13, 18, 0, 0), Hour), List("明晚6点", "明天晚上6点", "13号晚上6点")),
    (datetime(LocalDateTime.of(2013, 2, 12, 6, 0, 0), Hour), List("今早6点", "今天早上6点", "12号早上6点")),
    (datetime(LocalDateTime.of(2013, 2, 11, 20, 0, 0), Hour), List("昨晚8点", "昨天晚上8点", "这个月11号晚上8点")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 29, 58),
        LocalDateTime.of(2013, 2, 12, 4, 30, 0),
        Second
      ),
      List("上两秒", "上二秒", "前两秒", "前二秒", "上两秒")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 30, 0),
        LocalDateTime.of(2013, 2, 12, 4, 30, 3),
        Second
      ),
      List("下三秒", "后三秒")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 28, 0),
        LocalDateTime.of(2013, 2, 12, 4, 30, 0),
        Minute
      ),
      List("上两分钟", "上二分钟", "前两分钟", "前二分钟")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 30, 0),
        LocalDateTime.of(2013, 2, 12, 4, 33, 0),
        Minute
      ),
      List("下三分钟", "后三分钟")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 2, 0, 0),
        LocalDateTime.of(2013, 2, 12, 4, 0, 0),
        Hour
      ),
      List("上两小时", "上二小时", "前两小时", "前二小时")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 0, 0),
        LocalDateTime.of(2013, 2, 12, 7, 0, 0),
        Hour
      ),
      List("下三小时", "后三小时")
    ),
    (h(20), List("今晚8点", "今晚八点")),
    (hm(20, 30), List("今晚八点半")),
    (datetime(LocalDateTime.of(2013, 2, 13, 0, 0, 0), Hour), List("凌晨零点", "凌晨12点", "晚上12点")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 4, 30, 0),
        LocalDateTime.of(2013, 2, 12, 4, 45, 0),
        Minute
      ),
      List("未来一刻钟", "之后一刻钟", "向后一刻钟", "往后一刻钟")
    )
  )

  val weeks = List(
    (ymd(2013, 2, 18), List("星期一", "礼拜一", "周一")),
    (ymd(2013, 2, 12), List("星期二", "礼拜二", "周二")),
    (ymd(2013, 2, 13), List("星期三", "礼拜三", "周三")),
    (ymd(2013, 2, 14), List("星期四", "礼拜四", "周四")),
    (ymd(2013, 2, 15), List("星期五", "礼拜五", "周五")),
    (ymd(2013, 2, 16), List("星期六", "礼拜六", "周六")),
    (ymd(2013, 2, 17), List("星期日", "星期天", "礼拜日", "礼拜天", "周日", "周天")),
    (ymd(2013, 2, 10), List("上周日", "上星期天", "上礼拜天", "上星期天")),
    (ymd(2013, 2, 5), List("上周二", "上礼拜二", "上星期二")),
    (ymd(2013, 1, 29), List("上上周二")),
    (ymd(2013, 2, 13), List("这周三", "这礼拜三", "今个星期三", "今个礼拜三")),
    (ymd(2013, 2, 11), List("这周一", "这礼拜一", "今个星期一", "今个礼拜一")),
    (ymd(2013, 2, 19), List("下周二", "下星期二", "下礼拜二")),
    (ymd(2013, 2, 26), List("下下周二")),
    (ymd(2013, 2, 20), List("下周三", "下礼拜三", "下星期三")),
    (ymd(2013, 2, 12), List("这周二", "这礼拜二", "今个星期二", "今个礼拜二", "今星期二", "今礼拜二")),
    (ymd(2013, 2, 11, Week), List("这周", "本周", "这一周", "这礼拜", "这一礼拜")),
    (ymd(2013, 2, 4, Week), List("上周", "上个星期", "上个礼拜")),
    (ymd(2013, 2, 18, Week), List("下周", "下星期", "下礼拜")),
    (ymd(2013, 1, 21, Week), List("上上上周")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 16, 0, 0, 0),
        LocalDateTime.of(2013, 2, 18, 0, 0, 0),
        Day
      ),
      List("这周末")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 23, 0, 0, 0),
        LocalDateTime.of(2013, 2, 25, 0, 0, 0),
        Day
      ),
      List("下周末")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 11, 0, 0, 0),
        LocalDateTime.of(2013, 3, 4, 0, 0, 0),
        Week
      ),
      List("接下来三周", "下三个周")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 0, 0, 0),
        LocalDateTime.of(2013, 2, 19, 0, 0, 0),
        Day
      ),
      List("未来一周")
    ),
    (
      datetime(LocalDateTime.of(2013, 3, 5, 4, 30, 0), Second),
      List("三星期后", "三星期之后", "三个礼拜后", "三个礼拜之后", "三星期以后", "三星期过后")
    )
  )

  val holidays = List(
    (ymd(2014, 1, 1, holiday = "元旦节"), List("元旦", "元旦节", "阳历新年")),
    (ymd(2013, 3, 8, holiday = "妇女节"), List("妇女节")),
    (ymd(2013, 5, 1, holiday = "劳动节"), List("劳动节", "五一国际劳动节")),
    (ymd(2013, 6, 1, holiday = "儿童节"), List("61儿童节")),
    (ymd(2013, 8, 1, holiday = "建军节"), List("建军节", "八一建军节")),
    (ymd(2013, 12, 25, holiday = "圣诞节"), List("圣诞", "圣诞节")),
    (ymd(2013, 4, 1, holiday = "愚人节"), List("愚人节")),
    (ymd(2013, 11, 1, holiday = "万圣节"), List("万圣节")),
    (ymd(2013, 12, 20, holiday = "澳门回归纪念日"), List("澳门回归纪念日")),
    (ymd(2013, 2, 14, holiday = "情人节"), List("情人节", "圣瓦伦丁节")),
    (ymd(2013, 3, 15, holiday = "国际消费者权益日"), List("国际消费者权益日", "三一五")),
    (ymd(2013, 5, 12, holiday = "母亲节"), List("母亲节")),
    (ymd(2013, 8, 8, holiday = "台湾父亲节"), List("台湾父亲节")),
    (ymd(2013, 10, 1, holiday = "国庆节"), List("国庆节", "十一", "国庆")),
    (ymd(2013, 5, 1, holiday = "劳动节"), List("劳动节", "五一"))
  )

  val lunar = List(
    (ymd(2013, 8, 8, calendar = Lunar(false)), List("农历八月初八", "农历2013年八月八日", "八月初八")),
    (ymd(2012, 5, 10, calendar = Lunar(false)), List("去年五月初十")),
    (ymd(2013, 4, 4, holiday = "清明"), List("清明节")),
    (ymd(2019, 4, 5, holiday = "清明"), List("2019年的清明节")),
    (ymd(2016, 1, 1, holiday = "春节", calendar = Lunar(false)), List("大后年春节"))
  )

  val compose = List(
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 18, 4, 0, 0),
        LocalDateTime.of(2013, 2, 18, 12, 0, 0),
        Hour,
        partOfDay = "早上"
      ),
      List("周一早上", "周一早晨", "礼拜一早上", "礼拜一早晨", "下周一早上")
    ),
    (ymd(2013, 10, 7), List("十月第一个星期一", "十月的第一个星期一")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 15, 4, 0, 0),
        LocalDateTime.of(2013, 2, 15, 12, 0, 0),
        Hour,
        partOfDay = "早上"
      ),
      List("二月十五号早上", "二月十五号早晨", "2月15号早上", "2月15号早晨", "2013年二月十五号早上", "2.15早上")
    ),
    (datetime(LocalDateTime.of(2013, 3, 7, 15, 15, 0), Minute, "女生节"), List("女生节下午三点十五")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 14, 18, 0, 0),
        LocalDateTime.of(2013, 2, 15, 0, 0, 0),
        Hour,
        "情人节",
        partOfDay = "晚上"
      ),
      List("情人节晚上")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2018, 10, 1, 0, 0, 0),
        LocalDateTime.of(2018, 12, 1, 0, 0, 0),
        Month
      ),
      List("18年十月至十一月", "2018年十月到18年十一月")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 3, 0, 0, 0),
        LocalDateTime.of(2013, 3, 6, 0, 0, 0),
        Day
      ),
      List("本月三号到下月5号")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 12, 15, 0, 0),
        LocalDateTime.of(2013, 2, 12, 18, 0, 0),
        Hour
      ),
      List("下午三点到6点", "中午3点到6点")
    ),
    (
      localDateTimeInterval(
        LocalDateTime.of(2013, 2, 13, 15, 0, 0),
        LocalDateTime.of(2013, 2, 13, 18, 0, 0),
        Hour
      ),
      List("明天下午三点到6点")
    ),
    (datetime(LocalDateTime.of(2013, 3, 12, 19, 0, 0), Hour), List("一个月后下午19点")),
    (datetime(LocalDateTime.of(2018, 3, 30, 10, 10, 0), Minute), List("五年后3月三十号10点十分")),
    (
      localDateTimeInterval(
        LocalDateTime.of(2016, 7, 1, 19, 35, 0),
        LocalDateTime.of(2017, 3, 2, 10, 30, 0),
        Minute
      ),
      List("2016年7月一号晚上7点三十五分到2017年三月2号早上10点半")
    ),
    (ymd(2014, 11, 9), List("明年的11月份第二个周日")),
    (
      datetimeInterval(
        new DuckDateTime(LocalDateTime.of(2013, 2, 12, 0, 0, 0)),
        DuckDateTime(LunarDate(new LunarCalendar(2013, 8, 16, false)), LocalTime.of(0, 0), ZoneCN),
        Day
      ),
      List("今天到中秋节")
    ),
    (
      lunarDateTimeInterval(
        new LunarCalendar(2019, 12, 1, false),
        LocalTime.of(8, 0),
        new LunarCalendar(2019, 12, 1, false),
        LocalTime.of(12, 0),
        Hour,
        partOfDay = "上午"
      ),
      List("2019年腊月初一上午")
    ),
    (datetime(LocalDateTime.of(2013, 2, 11, 4, 30, 0), Second), List("昨天现在")),
    (datetime(LocalDateTime.of(2013, 2, 22, 8, 0, 0), Hour), List("下周五8点")),
    (datetime(LocalDateTime.of(2013, 11, 20, 20, 0, 0), Hour), List("11.20 20点")),
    (datetime(LocalDateTime.of(2013, 12, 24, 0, 0, 0), Day), List("圣诞节的前一天", "圣诞节前一天")),
    (ymd(2013, 10, 1, holiday = "国庆节"), List("下一个国庆节")),
    (ymd(2014, 1, 1, holiday = "元旦节"), List("下一个元旦节")),
    (ymd(2013, 8, 15, holiday = "中秋节", calendar = Lunar(false)), List("下一个中秋节")),
    (ymd(2012, 2, 12), List("一年前的今天")),
    (ymd(2022, 10, 1, direction = IntervalDirection.Before), List("2022年10月1号之前")),
    (ymd(2022, 10, 1, direction = IntervalDirection.After), List("2022年10月1号之后")),
    (h(12), List("今天12点"))
  )

  override def pairs: List[(ResolvedValue, List[String])] =
    (days ++ times ++ weeks ++ holidays ++ compose ++ lunar).map {
      case (expected, texts) => (expected, texts)
    }
}
