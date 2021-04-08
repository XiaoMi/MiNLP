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

package duckling.dimension.time.date

import scala.util.matching.Regex

import org.scalatest.{FunSpec, Matchers}

class RulesTest extends FunSpec with Matchers {

  object NoopRule extends Rules {}

  def groups(m: Regex.Match): String = {
    (0 to m.groupCount)
      .map(m.group)
      .map(s => if (s == "") "√" else s)
      .toList
      .mkString("[", ", ", "]")
  }

  describe("RulesTest") {

    import NoopRule._

    def exist(regex: Regex)(s: String): Boolean = {
      println(s"pattern => ${regex.pattern}")

      regex.findFirstMatchIn(s) match {
        case Some(x) => println(groups(x)); true
        case _ => false
      }
    }

    it("should ymdPattern") {
      val regex = exist(ymdPattern('.').regex) _
      regex("2013.2.12") shouldBe true
      regex("13.2.12") shouldBe true
      regex("15.3.3") shouldBe true
      regex("2013.2.121") shouldBe false
      regex(".2013.2.12") shouldBe false
      regex("20131.2.12") shouldBe false
    }

    it("should mdPattern") {
      val regex = exist(mdPattern('.').regex) _
      regex("2.12") shouldBe true
      regex("2.121") shouldBe false
      regex(".2.12") shouldBe false
      regex("2.12.") shouldBe false
    }

  }
}
