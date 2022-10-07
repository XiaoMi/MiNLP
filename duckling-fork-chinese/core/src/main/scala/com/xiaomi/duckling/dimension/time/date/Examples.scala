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

package com.xiaomi.duckling.dimension.time.date

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.time.enums.Lunar
import com.xiaomi.duckling.dimension.time.helper.TimeValueHelpers._

trait Examples extends DimExamples {

  override def pairs: List[(ResolvedValue, List[String])] = List(
    (ymd(2015, 3, 3), List("2015-3-3", "2015-03-03", "20150303", "2015/3/3", "2015.3.3", "15.3.3")),
    (y(2015), List("2015年", "2015版", "2015年版")),
    (y(2015), List("15年", "15版", "15年版")),
    (y(2015), List("一五年", "一五版", "一五年版")),
    (y(2006), List("06年", "06版", "06年版")),
    (y(2006), List("零六年", "零六版", "零六年版")),
    (y(1998), List("九八年", "一九九八年", "1998年", "98年", "一九九八版", "一九九八年版")),
    (md(3, 3), List("3.3", "03-03", "03/03")),
    (md(12, 15), List("十二月十五")),
    (ymd(2013, 3, 1), List("三月一号", "三月一日")),
    (ym(2013, 3), List("2013.03")),
    (
      ymd(2015, 3, 3),
      List("2015年3月3号", "2015年3月三号", "2015年三月3号", "2015年三月三号", "2015-3-3", "2015-03-03", "20150303")
    ),
    (ymd(2013, 2, 15), List("2013年2月15号", "2013年二月十五号", "2月15号", "二月十五号", "二月的十五号")),
    (y(2015), List("2015年")),
    (y(2015), List("15年")),
    (y(2015), List("一五年")),
    (y(2006), List("06年")),
    (y(2006), List("零六年")),
    (y(1998), List("九八年", "一九九八年", "1998年", "98年")),
    (md(5, 25), List("5月25号")),
    (ym(2013, 5), List("五月")),
    (ym(1988, 5), List("1988年五月")),
    (ym(2001, 5), List("零一年五月")),
    (ymd(2013, 2, 28), List("月底")),
    // 2013.2.12 农历是 2013年正月初三 => 2014年正月初二
    (ymd(2014, 1, 2, calendar = Lunar(false)), List("农历一月初二", "正月初二", "农历的一月初二", "阴历的正月初二", "一月初二农历", "一月初二的农历")),
    (ymd(2013, 1, 18, calendar = Lunar(false)), List("农历一月十八", "正月十八")),
    (ymd(2013, 11, 8, calendar = Lunar(false)), List("农历十一月初八", "冬月初八")),
    (ymd(2013, 11, 22, calendar = Lunar(false)), List("农历十一月二十二", "冬月二十二")),
    (ymd(2013, 12, 10, calendar = Lunar(false)), List("农历十二月初十", "腊月初十")),
    (ymd(2013, 12, 13, calendar = Lunar(false)), List("农历十二月十三", "腊月十三")),
    (ymd(2013, 8, 8, calendar = Lunar(false)), List("农历八月初八", "农历2013年八月八日", "八月初八"))
  )
}
