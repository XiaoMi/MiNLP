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

package duckling.dimension.currency

import duckling.Api.analyze
import duckling.dimension.quantity.QuantityValue
import duckling.ranking.Testing.{testContext, testOptions}
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSpec, Matchers}

class CurrencyTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

	val options = testOptions.copy(targets = Set(Currency), full = false)

	describe("CurrencySchemaTest") {
		val testCases = List(
			("九十九元九角九分", Some("99.99000000000001")),
			("九十九块九毛九", Some("99.99")),
			("九十九块九", Some("99.9")),
			("九块九", Some("9.9")),
			("九角九分", Some("0.99")),
			("九毛钱", Some("0.9")),
			("九分钱", Some("0.09"))
		)

		it("schema eq") {
			testCases.foreach{
				case (query, target) =>
					val answers = analyze(query, testContext,	options)
					answers(0).token.value match {
						case data: QuantityValue  => data.schema shouldBe target
						case _ => true shouldBe (false)
					}
			}
		}
	}
}
