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

package com.xiaomi.duckling.dimension.level

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.dimension.numeral.NumeralValue
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSpec, Matchers}

class LevelTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

	val options = testOptions.copy(targets = Set(Level), full = false)

	describe("LevelSchemaTest") {
		val testCases = List(
			("第五档", Some("5.0")),
			("第二级", Some("2.0")),
			("三十3级", Some("33.0")),
			("十级大风", Some("10.0")),
			("三档", Some("3.0"))
		)

		it("schema eq") {
			testCases.foreach{
				case (query, target) =>
					val answers = analyze(query, testContext,	options)
					answers(0).token.value match {
						case data: NumeralValue  => data.schema shouldBe target
						case _ => true shouldBe false
					}
			}
		}
	}
}
