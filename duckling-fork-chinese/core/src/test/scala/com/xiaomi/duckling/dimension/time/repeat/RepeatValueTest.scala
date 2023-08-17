package com.xiaomi.duckling.dimension.time.repeat

import com.xiaomi.duckling.UnitSpec
import com.xiaomi.duckling.ranking.Testing.{testContext, testOptions}
import com.xiaomi.duckling.Api.analyze
import com.xiaomi.duckling.Types.Context

class RepeatValueTest extends UnitSpec {

  private val options = testOptions.copy(targets = Set(Repeat))

  private def parse(sentence: String, context: Context = testContext) = {
    analyze(sentence, context, options)
  }

  describe("RepeatValueTest") {

    val cases = Table(("query", "schema")
      , ("每个工作日上午", "Repeat_Hour_08:00/12:00_Workday")
      , ("每个非工作日上午八点", "Repeat_Hour_08:00_NonWorkday")
      , ("每月2号下午2点", "Repeat_Hour_2013-03-02T14:00_P1M")
    )

    it("should schema") {
      forAll(cases) { (query, schema) =>
        val a = parse(query)
        a.head.token.value match {
          case tv: RepeatValue if tv.schema.nonEmpty =>
            tv.schema.get shouldBe schema
          case _ => true shouldBe false
        }
      }
    }
  }
}
