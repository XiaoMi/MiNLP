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

package duckling.dimension.season

import duckling.Api.analyze
import duckling.dimension.quantity.QuantityValue
import duckling.ranking.Testing.{testContext, testOptions}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSpec, Matchers}

class SeasonTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

	val options = testOptions.copy(targets = Set(Season), full = false)

	describe("SeasonSchemaTest") {
		val testCases = List(
			("倒数第三季", Some("-3.0")),
			("第三部", Some("3.0")),
			("第一百一十一卷", Some("111.0")),
			("第九版", Some("9.0")),
			("第三册", Some("3.0")),
			("第十九季", Some("19.0")),
			("最新一部", Some("-1.0"))
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
}
