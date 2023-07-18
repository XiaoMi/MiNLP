package com.xiaomi.duckling

import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.ranking.Testing

class DuckParserTest extends UnitSpec {

  describe("DuckParserTest") {
    it("should analyze") {
      val parser = new DuckParser(Set("Time"))
      println(parser.analyze("今天", Testing.testContext, Testing.testOptions.copy(targets = Set(Time))))
    }
  }
}
