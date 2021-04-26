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

package com.xiaomi.duckling.engine

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types.{Range, Token}
import com.xiaomi.duckling.dimension.matcher.{Phrase, PhraseMatch}
import com.xiaomi.duckling.types.Node

/**
  * 由1个或多个切词得到的token组成
  */
object PhraseLookup extends LazyLogging {

  type PhraseMatcherFn = String => Boolean

  def lookupPhraseAnywhere(doc: Document,
                           position: Int,
                           phraseFn: PhraseMatcherFn,
                           min: Int = 1,
                           max: Int): List[Node] = {
    lookupPhrase(doc, position = 0, phraseFn = phraseFn, min = min, max = max, anywhere = true)
  }

  def lookupPhrase(doc: Document,
                   position: Int,
                   phraseFn: PhraseMatcherFn,
                   min: Int = 1,
                   max: Int,
                   anywhere: Boolean = false): List[Node] = {
    if (doc.firstNonAdjacent.length <= position) Nil
    else {
      val adjustPos = doc.firstNonAdjacent(position)
      val index = doc.indexOfToken(adjustPos)
      if (index == -1) Nil // 该位置与切词冲突
      else {
        val (_, rangeToText, translateRange) = doc.stringFromPos(doc.token(index).start)

        val f = (bsStart: Int, bsLen: Int) => {
          val text = rangeToText(bsStart, bsLen)
          val (start, end) = translateRange(bsStart, bsLen)
          Node(
            range = Range(start, end),
            token = Token(PhraseMatch, Phrase(text)),
            children = Nil,
            rule = None,
            production = null
          )
        }

        val upper = if (anywhere) doc.numTokens - 1 else index

        val pairs = (for {
          s <- index to upper
          t <- s + min to math.min(s + max, doc.numTokens)
        } yield {
          val p = doc.phrase(s, t)
          if (phraseFn(p)) {
            val start = doc.token(s).start - adjustPos
            val len = doc.token(t - 1).end - doc.token(s).start
            Some(start, len)
          } else None
        }).flatten
        pairs.map(f.tupled).toList
      }
    }
  }
}
