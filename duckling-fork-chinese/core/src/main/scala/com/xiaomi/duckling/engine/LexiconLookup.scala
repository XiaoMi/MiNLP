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

import java.util

import scala.collection.JavaConverters._
import scala.collection.mutable

import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie
import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types.{Range, Token}
import com.xiaomi.duckling.dimension.matcher.{LexiconMatch, LexiconMatches}
import com.xiaomi.duckling.types.Node

/**
  * 根据提供的词表进行匹配，使用HanLP的AC自动机实现
  */
object LexiconLookup extends LazyLogging {

  case class Dict(var vocab: AhoCorasickDoubleArrayTrie[String], maximumOnly: Boolean = false) {
    def this(tree: util.TreeMap[String, String], maximumOnly: Boolean) = {
      this({
        val _vocab = new AhoCorasickDoubleArrayTrie[String]()
        _vocab.build(tree)
        _vocab
      }, maximumOnly)
    }
  }

  def lookupLexiconAnywhere(doc: Document, position: Int, dict: Dict): List[Node] = {
    lookupLexicon(doc, position = position, dict, anywhere = true)
  }

  def lookupLexicon(doc: Document,
                    position: Int,
                    dict: Dict,
                    anywhere: Boolean = false): List[Node] = {
    if (doc.firstNonAdjacent.length <= position) Nil
    else {
      val f = (bsStart: Int, word: String, target: String) => {
        Node(
          range = Range(bsStart, bsStart + word.length),
          token = Token(LexiconMatch, LexiconMatches(word, target)),
          children = Nil,
          rule = None,
          production = null
        )
      }

      val positions =
        if (anywhere) dict.vocab.parseText(doc.rawInput).asScala
        else {
          val part = doc.rawInput.substring(position)
          dict.vocab.parseText(part).asScala.filter(_.begin == position)
        }
      val filtered =
        if (dict.maximumOnly) {
          remainMaximumOnly(positions)
        } else positions
      filtered.map(hit => (hit.begin, doc.rawInput.substring(hit.begin, hit.end), hit.value)).map(f.tupled).toList
    }
  }

  def remainMaximumOnly(hits: mutable.Buffer[AhoCorasickDoubleArrayTrie.Hit[String]]): mutable.Buffer[AhoCorasickDoubleArrayTrie.Hit[String]] = {
    if (hits.isEmpty) hits
    else {
      val sorted = hits.sortWith((a, b) => !(a.begin < b.begin || a.begin == b.begin && a.end <= b.begin))
      val buffer = mutable.Buffer[AhoCorasickDoubleArrayTrie.Hit[String]](sorted.head)
      sorted.tail.foldLeft(buffer) { (b, hit) =>
        val last = b.last
        if (!(last.begin == hit.begin && last.end >= hit.end)) {
          b.append(hit)
        }
        b
      }
    }
  }
}
