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
import com.xiaomi.duckling.dimension.matcher.{MultiCharMatch, MultiCharMatches}
import com.xiaomi.duckling.types.Node

/**
  * 寻找双字符（不是字节）符号，比如emoji
  */
object MultiCharLookup extends LazyLogging {

  def lookupMultiCharAnywhere(doc: Document, position: Int): List[Node] = {
    lookupMultiChar(doc, position = position, anywhere = true)
  }

  def lookupMultiChar(doc: Document, position: Int, anywhere: Boolean = false): List[Node] = {
    if (doc.firstNonAdjacent.length <= position) Nil
    else {
      val f = (bsStart: Int) => {
        val text = doc.substring(bsStart, bsStart + 2)
        Node(
          range = Range(bsStart, bsStart + 2),
          token = Token(MultiCharMatch, MultiCharMatches(text)),
          children = Nil,
          rule = None,
          production = null
        )
      }

      val positions = if (anywhere) {
        (position until doc.length).filter(i => charCount(doc.rawInput, i) == 2).toList
      } else if (charCount(doc.rawInput, position) == 2) List(position)
      else Nil

      positions.map(f)
    }
  }

  def charCount(s: String, i: Int): Int = {
    Character.charCount(s.codePointAt(i))
  }
}
