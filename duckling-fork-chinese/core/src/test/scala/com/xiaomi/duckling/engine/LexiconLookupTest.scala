package com.xiaomi.duckling.engine

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.dimension.gender.Gender
import org.scalatest.{FunSpec, Matchers}

class LexiconLookupTest extends FunSpec with Matchers {

  describe("LexiconLookupTest") {
    it("should lookupLexiconAnywhere") {
      val doc = Document.fromText("处女座的女明星")
      LexiconLookup.lookupLexiconAnywhere(doc, 0, Gender.dict).size shouldBe 2
    }
  }
}
