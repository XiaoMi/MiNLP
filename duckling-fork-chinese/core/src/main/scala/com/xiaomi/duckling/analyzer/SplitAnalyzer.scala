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

import scala.collection.mutable

import com.robrua.nlp.bert.BasicTokenizer
import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.types.{LanguageInfo, TokenLabel}

/**
  * 使用了BERT的切字
  */

class SplitAnalyzer extends Analyzer with LazyLogging {
  val tokenizer = new BasicTokenizer(false)

  override def analyze(sentence: String): LanguageInfo = {
    val words = tokenizer.tokenize(sentence)
    val buf = mutable.Buffer[TokenLabel]()
    words.foreach { word =>
      val pos = buf.lastOption.map(_.end).getOrElse(0)
      val start = sentence.indexOf(word, pos)
      if (start == -1) {
        logger.warn(s"${word} not found in '${sentence}:${start}''")
      } else {
        buf.append(TokenLabel(buf.size + 1, word, start, start + word.length, "o"))
      }
    }
    LanguageInfo(sentence, buf.toArray)
  }
}
