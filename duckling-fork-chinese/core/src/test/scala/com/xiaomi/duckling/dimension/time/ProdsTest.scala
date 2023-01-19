package com.xiaomi.duckling.dimension.time

import com.xiaomi.duckling.{Api, UnitSpec}
import com.xiaomi.duckling.Types.Options
import com.xiaomi.duckling.ranking.Testing.testContext

class ProdsTest extends UnitSpec {

  describe("ProdsTest") {
    it("should limitedSequenceByRange") {
      val options = Options(withLatent = false, targets = Set(Time))
      options.rankOptions.setWinnerOnly(false)
      options.rankOptions.setSequence1EndsPrune(true)
      options.rankOptions.setNodesLimit(99999)

      val start = System.currentTimeMillis()
      val query = (0 until 32).map(_ => "明天").mkString("的")
      val answers = Api.analyze(query, testContext, options)
      val end = System.currentTimeMillis()
      println(s"$query => |${answers.length}|, cost = ${end - start}")
      answers.length shouldBe (32 + 31) // 可以从528 优化到 63
    }
  }
}
