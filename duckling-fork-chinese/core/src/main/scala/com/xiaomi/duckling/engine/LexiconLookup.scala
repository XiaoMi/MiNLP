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

import com.google.common.collect.Multimap
import com.typesafe.scalalogging.LazyLogging

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types.{Range, Token}
import com.xiaomi.duckling.dimension.matcher.{LexiconMatch, LexiconMatches}
import com.xiaomi.duckling.types.Node

/**
  * 根据提供的词表进行匹配，目前实现得非常简单，如果有大词表还需要进行改进
  */
object LexiconLookup extends LazyLogging {

  case class Dict(var vocab: Multimap[String, String])

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

      // 这里的key中可能有重复，用于命中更多选项
      val positions = dict.vocab
        .keySet()
        .asScala
        .flatMap { w =>
          val indices =
            if (anywhere) all(doc.rawInput, w).toList
            else if (doc.rawInput.indexOf(w, position) == position) List(position)
            else Nil
          if (indices.isEmpty) Nil
          else {
            dict.vocab.get(w).asScala.flatMap(t => indices.map((_, w, t)))
          }
        }
        .toList
      positions.map(f.tupled)
    }
  }

  /**
    * 在字符串中寻找所有的匹配位置
    *
    * @param text    待查询文本
    * @param w       目标
    * @param start   起始位置
    * @param indices 目标所在的坐标
    * @return
    */
  @tailrec
  def all(text: String, w: String, start: Int = 0, indices: mutable.Buffer[Int] = mutable.Buffer[Int]()): mutable.Buffer[Int] = {
    val i = text.indexOf(w, start)
    if (i != -1) {
      indices.append(i)
      all(text, w, i + w.length, indices)
    } else {
      indices
    }
  }
}
