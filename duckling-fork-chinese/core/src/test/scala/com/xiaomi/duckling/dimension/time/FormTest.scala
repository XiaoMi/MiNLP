package com.xiaomi.duckling.dimension.time

import com.xiaomi.duckling.{Api, UnitSpec}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.time.form.{Form, PartOfDay, TimeOfDay}
import com.xiaomi.duckling.ranking.Testing

class FormTest extends UnitSpec {
  describe("Form") {

    val cases = Table[String, Option[Form]](("query", "form")
      , ("23号上午8点", TimeOfDay(8, false))
      , ("8点", TimeOfDay(8, true))
      , ("上午8点", TimeOfDay(8, false))
      , ("23号8点", None)
      , ("23号上午", PartOfDay("上午"))
      , ("明天上午", PartOfDay("上午"))
    )

    it("form test") {
      forEvery(cases) { (query, form) =>
        val answers = Api.analyze(query, Testing.testContext, Testing.testOptions.copy(targets = Set(Time)))
        answers should not be empty
        answers.head.token.node.token.data shouldBe a[TimeData]
        val tv = answers.head.token.node.token.data.asInstanceOf[TimeData]
        tv.form shouldBe form
      }
    }
  }
}
