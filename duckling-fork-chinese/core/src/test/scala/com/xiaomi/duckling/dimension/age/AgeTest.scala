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

package com.xiaomi.duckling.dimension.age

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.dimension.numeral.{DoubleSideIntervalValue, NumeralValue, OpenIntervalValue}
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.UnitSpec

class AgeTest extends UnitSpec {

	val options = testOptions.copy(targets = Set(Age), full = false)

	describe("AgeSchemaTest") {
		val testCases = List(
			("小明今年六岁", Some("6.0")),
			("八岁半", Some("8.5")),
			("小于十八岁半", Some("<18.5")),
			("一到十八岁", Some("1.0<18.0")),
			("不超过五岁", Some("<5.0")),
			("十岁以上", Some(">10.0")),
			("三岁到五岁半之间", Some("3.0<5.5"))
		)

		it("schema eq") {
			testCases.foreach{
				case (query, target) =>
					val answers = analyze(query, testContext,	options)
					answers(0).token.value match {
						case data: DoubleSideIntervalValue  => data.schema shouldBe target
						case data: NumeralValue  => data.schema shouldBe target
						case data: OpenIntervalValue  => data.schema shouldBe target
						case _ => true shouldBe (false)
					}
			}
		}
	}
}
