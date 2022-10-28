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

package com.xiaomi.duckling.dimension.time.duration

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.DimExamples
import com.xiaomi.duckling.dimension.time.enums.Grain._

trait Examples extends DimExamples {

  override def pairs: List[(ResolvedValue, List[String])] = List(
    (DurationData(1, Second, schema = Some("PT1S")), List("1秒钟", "1秒")),
    (DurationData(69, Second, schema = Some("PT1M9S")), List("1分09秒", "一分零九秒")),
    (DurationData(185, Minute, schema = Some("PT3H5M")), List("3小时05分", "三小时零五分钟")),
    (DurationData(90, Second, schema = Some("PT1M30S")), List("1分半", "1分半钟")),
    (DurationData(2, Minute, schema = Some("PT2M")), List("2分钟", "两分钟", "二分钟")),
    (DurationData(69, Minute, schema = Some("PT1H9M")), List("1小时09分", "1小时9分", "一小时零九分", "一小时九分")),
    (DurationData(30, Day, schema = Some("P30D")), List("30天")),
    (DurationData(7, Week, schema = Some("P7W")), List("七周")),
    (DurationData(1, Month, schema = Some("P1M")), List("一个月")),
    (DurationData(3, Month, latent = true, fuzzy = true, schema = Some("P3M")), List("几个月")),
    (DurationData(3, Quarter, schema = Some("P3Q")), List("3个季度")),
    (DurationData(2, Year, schema = Some("P2Y")), List("两年", "2年")),
    (DurationData(2000, Year, schema = Some("P2000Y")), List("两千年")),
    (DurationData(30, Minute, schema = Some("PT0H30M")), List("半小时")),
    (DurationData(30, Minute, schema = Some("PT30M")), List("0.5小时")),
    (DurationData(30, Minute, schema = Some("PT30M")), List("30分钟")),
    (DurationData(12, Hour, schema = Some("P0DT12H")), List("半天")),
    (DurationData(90, Minute, schema = Some("PT1H30M")), List("一个半小时", "一小时30分钟", "一个小时30分钟")),
    (DurationData(2, Hour, schema = Some("PT2H")), List("两个小时")),
    (DurationData(45, Day, schema = Some("P1M15D")), List("一个半月")),
    (DurationData(15, Day, schema = Some("P0M15D")), List("半个月")),
    (DurationData(27, Month, schema = Some("P2Y3M")), List("两年零三个月","两年外加三个月", "两年加上三个月", "两年加三个月", "两年三个月")),
    (DurationData(31719604, Second, schema = Some("P1Y2DT3H4S")), List("1年两天3小时四秒"))
  )

}
