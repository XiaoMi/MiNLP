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

package duckling.dimension.music

import org.scalatest.{FunSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks

class LyricTest extends FunSpec with Matchers with TableDrivenPropertyChecks {

  describe("LyricTest") {

    it("should RolePattern") {
      val regex = Lyric.RolePattern.r
      regex.findFirstIn("曲∶常田真太郎") should contain("曲")
      regex.findFirstIn("Rap词:中田ヤスタカ(capsule)") should contain("Rap词")
    }
  }
}
