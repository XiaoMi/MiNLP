package com.xiaomi.duckling.engine

import com.xiaomi.duckling.{Document, UnitSpec}
import com.xiaomi.duckling.dimension.gender.Gender


class LexiconLookupTest extends UnitSpec {

  describe("LexiconLookupTest") {
    it("should lookupLexiconAnywhere") {
      val doc = Document.fromText("处女座的女明星")
      LexiconLookup.lookupLexiconAnywhere(doc, 0, Gender.dict).size shouldBe 2
    }
  }
}
