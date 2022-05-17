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

package com.xiaomi.duckling.dimension.time.helper

import com.google.common.collect.{ImmutableListMultimap, ImmutableTable, Maps, Table}
import java.time.LocalDate

import com.xiaomi.duckling.Resources
import com.xiaomi.duckling.Types.conf
import com.xiaomi.duckling.engine.LexiconLookup.Dict

trait SolarTermProvider {

  /**
    * 到归一化值的映射
    */
  def dict: Dict

  /**
    * 节气按年分的查找表
    * @return
    */
  def solarTermTable: Table[Int, String, LocalDate]
}

class LocalSolarTermProvider extends SolarTermProvider {
  private val loadFrom = conf.getString("dimension.time.solar.days.load-from")
  private val file = "solar_terms.csv"

  def build(): Table[Int, String, LocalDate] = {
    val builder = ImmutableTable.builder[Int, String, LocalDate]()
    val lines = loadFrom match {
      case "remote"   => Resources.readLinesFromUrl(file)
      case "resource" => Resources.readLines(file)
      case _          => throw new RuntimeException(s"unknown source [$loadFrom]")
    }
    lines.foreach { line =>
      if (!line.startsWith("#")) {
        val terms = line.trim.split(",")
        if (terms.length == 3) {
          val year = terms(0).toInt
          val month = terms(1).substring(0, 2).toInt
          val day = terms(1).substring(2, 4).toInt
          val solarTerm = terms(2)
          builder.put(year, solarTerm, LocalDate.of(year, month, day))
        }
      }
    }
    builder.build()
  }

  private val table = build()

  /**
    * 节气规律比较弱，查表实现
    */
  override def solarTermTable: Table[Int, String, LocalDate] = table

  private val mmap = {
    val builder = Maps.newTreeMap[String, String]()
    builder.put("立春", "立春")
    builder.put("雨水", "雨水")
    builder.put("惊蛰", "惊蛰")
    builder.put("春分", "春分")
    builder.put("清明节", "清明")
    builder.put("清明", "清明")
    builder.put("谷雨", "谷雨")
    builder.put("立夏", "立夏")
    builder.put("小满", "小满")
    builder.put("芒种", "芒种")
    builder.put("夏至", "夏至")
    builder.put("小暑", "小暑")
    builder.put("大暑", "大暑")
    builder.put("立秋", "立秋")
    builder.put("处暑", "处暑")
    builder.put("白露", "白露")
    builder.put("秋分", "秋分")
    builder.put("寒露", "寒露")
    builder.put("霜降", "霜降")
    builder.put("立冬", "立冬")
    builder.put("小雪", "小雪")
    builder.put("大雪", "大雪")
    builder.put("冬至", "冬至")
    builder.put("小寒", "小寒")
    builder.put("大寒", "大寒")
    builder
  }

  override def dict = new Dict(mmap, false)
}
