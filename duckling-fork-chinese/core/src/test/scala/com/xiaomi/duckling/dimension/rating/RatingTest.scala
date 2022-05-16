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

package com.xiaomi.duckling.dimension.rating

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.dimension.answerSize
import com.xiaomi.duckling.dimension.numeral.{DoubleSideIntervalValue, NumeralValue, OpenIntervalValue}
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.UnitSpec

class RatingTest extends UnitSpec {

	val options = testOptions.copy(targets = Set(Rating), full = false)

	describe("Rating Schema Test") {
		val testCases = List(
			("评分8点5分", Some("8.5")),
			("评分大于八点五", Some(">8.5")),
			("评分超过七分", Some(">7.0")),
			("一到八分", Some("1.0<8.0")),
			("评分九点零以下", Some("<9.0")),
			("十分以上", Some(">10.0")),
			("评分在7到8.5分", Some("7.0<8.5"))
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

	describe("rating answer size") {
		it("size == 1") {
			answerSize("评分九点零以下", Set(Rating)) shouldBe 1
		}
	}

}
