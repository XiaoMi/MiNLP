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

package com.xiaomi.duckling.dimension.time.rule

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.singleRegexMatch
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.TimeData
import com.xiaomi.duckling.dimension.time.enums.Grain.Day
import com.xiaomi.duckling.dimension.time.enums.Lunar
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.{EndOfGrainPredicate, SequencePredicate}

object LunarDays {

	val rulePeriodicHolidays: List[(Token, String, String)] = mkRuleLunarDays(
		List(
			(lunarMonthDay(1, 1), "春节", "春节|过年|大年初一"),
			(lunarMonthDay(1, 15), "元宵节", "元宵节|元宵"),
			(lunarMonthDay(2, 2), "龙抬头", "龙抬头"),
			(lunarMonthDay(5, 5), "端午节", "端午节|端午"),
			(lunarMonthDay(6, 24), "火把节", "火把节"),
			(lunarMonthDay(7, 7), "七夕情人节", "七夕情人节|七夕情人|七夕节|七夕"),
			(lunarMonthDay(7, 15), "中元节", "中元节|鬼节"),
			(lunarMonthDay(8, 15), "中秋节", "中秋节|中秋"),
			(lunarMonthDay(9, 9), "重阳节", "重阳节|重阳"),
			(lunarMonthDay(12, 8), "腊八节", "腊八节|腊八"),
			(lunarMonthDay(12, 23), "小年", "北方小年|小年"),
			(lunarMonthDay(12, 24), "南方小年", "南方小年"),
			(lunarMonthDay(12, 30), "除夕", "大年三十")
		)
	)

	def lunarMonthDay(m: Int, d: Int): TimeData = {
		lunar(monthDay(m, d))
	}

	def mkRuleLunarDays(list: List[(TimeData, String, String)]): List[(Token, String, String)] = {
		list.map {
			case (td, name, ptn) =>
				val token = tt(td.copy(okForThisNext = true, holiday = name, schema = Some(s"[EXT][FEST]FEST_$name")))
				(token, name, ptn)
		}
	}

	/**
	 * 除夕可能是12/29也可能是12/30，定义为12月的最后一天
	 */
  val ruleLunarNewYearsEve = Rule(
    name = "<农历年最后一天>",
    pattern = List("除夕|大年夜".regex),
    prod = singleRegexMatch { case _ =>
      val cal = Some(Lunar(false))
      val td1 = TimeData(EndOfGrainPredicate, timeGrain = Day, calendar = cal)
      val td = TimeData(SequencePredicate(List(lunar(month(12)), td1)), timeGrain = Day, calendar = cal)
      Some(tt(td.copy(okForThisNext = true, holiday = "除夕", schema = Some("[EXT][FEST]FEST_除夕"))))
    }
  )
}