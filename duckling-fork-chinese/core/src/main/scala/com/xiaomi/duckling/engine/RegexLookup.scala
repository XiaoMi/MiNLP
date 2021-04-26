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

import scala.util.matching.Regex

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types.{Range, Token}
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.types.Node

object RegexLookup {

  /**
    * string: start len
    */
  type MatchArray = List[(Int, Int)]

  /**
    * Handle a regex match at a given position
    *
    * @param doc
    * @param regex
    * @param position
    * @return
    */
  def lookupRegex(doc: Document, regex: Regex, position: Int): List[Node] = {
    if (position > doc.length) Nil
    else lookupRegexCommon(doc, regex, position, matchOnce)
  }

  def lookupRegexCommon(doc: Document,
                        regex: Regex,
                        position: Int,
                        matchFun: (Regex, CharSequence) => List[MatchArray]): List[Node] = {
    val (substring, rangeToText, translateRange) = doc.stringFromPos(position)

    def f(list: MatchArray): Option[Node] = list match {
      case Nil         => None
      case (0, 0) :: _ => None
      // Haskell中的Regex库返回第一个元素为整个匹配项
      case groups @ (g1 @ (bsStart, bsLen) :: tail) =>
        val textGroups = groups.map(rangeToText.tupled)
        val (start, end) = translateRange(bsStart, bsLen)
        if (doc.isRangeValid(start, end)) {
          val node = Node(
            range = Range(start, end),
            token = Token(RegexMatch, GroupMatch(textGroups)),
            children = Nil,
            rule = None,
            production = null
          )
          Some(node)
        } else None
    }

    matchFun(regex, substring).flatMap(f)
  }

  def matchOnce(regex: Regex, cs: CharSequence): List[MatchArray] = {
    regex
      .findFirstMatchIn(cs)
      .map(m => List(groups(m)))
      .getOrElse(Nil)
  }

  def groups(m: scala.util.matching.Regex.Match): MatchArray = {
    val n = m.groupCount
    (0 to n).toList.map(i => (m.start(i), m.end(i) - m.start(i)))
  }

  def lookupRegexAnywhere(doc: Document, regex: Regex): List[Node] = {
    lookupRegexCommon(doc, regex, 0, matchAll)
  }

  def matchAll(regex: Regex, cs: CharSequence): List[MatchArray] = {
    val matches = regex.findAllMatchIn(cs).toList
    matches.map(groups)
  }
}
