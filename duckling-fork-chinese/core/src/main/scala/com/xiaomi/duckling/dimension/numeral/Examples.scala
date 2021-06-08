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

package com.xiaomi.duckling.dimension.numeral

import com.xiaomi.duckling.Types.ResolvedValue
import com.xiaomi.duckling.dimension.DimExamples

trait Examples extends DimExamples {
  lazy val list: List[(Double, List[String])] = List(
    (0, List("0", "〇", "零")),
    (1, List("1", "一")),
    (2, List("2")),
    (3, List("Ⅲ")),
    (10, List("10", "十")),
    (11, List("11", "十一")),
    (20, List("20", "二十")),
    (60, List("60", "六十")),
    (33, List("33", "三十三")),
    (96, List("96", "九十六")),
    (203, List("203", "二百零三")),
    (534, List("534", "五百三十四")),
    (730, List("七百三", "七百三十")),
    (734, List("七百三十四")),
    (34567, List("34567", "34,567", "三万四千五百六十七")),
    (10040, List("10040", "10,040", "一万零四十")),
    (34507, List("34507", "34,507", "三万四千五百零七")),
    (100000, List("100,000", "100000", "100K", "100k", "十万")),
    (3000000, List("3M", "3000000", "3,000,000", "三百万")),
    (1040000, List("1,040,000", "1040000", "1.04M", "一百零四万")),
    (1200000, List("1,200,000", "1200000", "1.2M", ".0012G", "一百二十万")),
    (
      -1200000,
      List(
        "- 1,200,000",
        "-1200000",
        "负1,200,000",
        "负 1,200,000",
        "负1200000",
        "负 1200000",
        "-1.2M",
        "-1200K",
        "-.0012G",
        "负一百二十万"
      )
    ),
    (1100009032L, List("十一亿零九千零三十二")),
    (8700, List("八千七")),
    (530, List("五百三")),
    (1000000, List("壹百万")),
    (12034, List("一万两千零3十四")),
    (6694894669L, List("6694894669"))
  )

  val decimals = List(
    ((1.1, 1), List("1.1", "1点1", "一点一", "1点一")),
    ((1.1, 2), List("1.10")),
    ((0.77, 2), List("0.77", ".77")),
    ((0.00285, 5), List("零零点零零二八五")),
    ((72.0, 1), List("72.0", "七十二点零")),
    ((7.1, 1), List("7.1")),
    ((1.45, 2), List("一点四五")), // 加入错误念法
    ((1.45, 2), List("1点45")),
    ((1.4, 1), List("一点四")),
    ((1987.43, 2), List("一千九百八十七点四三")),
    ((123.123, 3), List("123.123"))
  )

  override def pairs: List[(ResolvedValue, List[String])] = {
    list.map {
      case (expected, texts) => (NumeralValue(expected), texts)
    } ++ decimals.map {
      case ((value, precision), texts) => (NumeralValue(value, precision), texts)
    }
  }
}
