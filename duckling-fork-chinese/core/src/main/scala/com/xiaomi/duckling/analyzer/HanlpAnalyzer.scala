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

package com.xiaomi.duckling.analyzer

import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.tokenizer.StandardTokenizer

import scala.collection.JavaConverters._

import com.xiaomi.duckling.types
import com.xiaomi.duckling.types.{LanguageInfo, TokenLabel}

/**
  * 使用了HanLP的切词，依存有需要也可以增加
  */
class HanlpAnalyzer extends Analyzer {

  StandardTokenizer.SEGMENT.enableIndexMode(true)

  override def analyze(sentence: String): LanguageInfo = {
    val terms = HanLP.segment(sentence)

    val tokens = terms.asScala.zipWithIndex.map {
      case (term, i) =>
        TokenLabel(
          id = i + 1,
          word = term.word,
          start = term.offset,
          end = term.offset + term.length(),
          tag = term.nature.toString
        )
    }.toArray
    types.LanguageInfo(sentence, tokens, Map())
  }
}
