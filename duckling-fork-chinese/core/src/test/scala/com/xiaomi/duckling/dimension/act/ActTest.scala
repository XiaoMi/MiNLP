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

package com.xiaomi.duckling.dimension.act

import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.UnitSpec
import com.xiaomi.duckling.dimension.quantity.QuantityValue
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}

class ActTest extends UnitSpec {

	val options = testOptions.copy(targets = Set(Act), full = false)

	describe("SeasonSchemaTest") {
		val testCases = List(
			("倒数第三场", Some("-3.0")),
			("第三幕", Some("3.0")),
			("第一百一十一弹", Some("111.0")),
			("第九弹", Some("9.0")),
			("第三番", Some("3.0")),
			("第十九番", Some("19.0")),
			("最新一幕", Some("-1.0"))
		)

		it("schema eq") {
			testCases.foreach{
				case (query, target) =>
					val answers = analyze(query, testContext, options)
					answers(0).token.value match {
						case data: QuantityValue  => data.schema shouldBe target
						case _ => true shouldBe (false)
					}
			}
		}
	}

	describe("SeasonLatentTest") {
		val testCases = List(
			("倒数第三场", false),
			("十九幕", true),
			("第三幕", false),
			("倒第三弹", false),
			("第一百一十一幕", false),
			("九番", true),
			("一百一十一场", true)
		)

		it("Latent eq") {
			testCases.foreach{
				case (query, target) =>
					val answers = analyze(query, testContext, options)
					answers(0).token.isLatent shouldBe (target)
			}
		}
	}
}
