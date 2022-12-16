package com.xiaomi.duckling.dimension

import com.xiaomi.duckling.{Api, UnitSpec}
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.ranking.Testing.testContext
import com.xiaomi.duckling.Types.Options

class LiteDimensionsTest extends UnitSpec {
  describe("LiteTest") {
    it("should analyze") {
      val options = Options(targets = Set(Time, Numeral))
      Api.analyze("今天的天气怎么样123", testContext, options) should have size 2
    }
  }
}
