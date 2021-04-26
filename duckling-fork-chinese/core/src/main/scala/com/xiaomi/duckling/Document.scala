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

package com.xiaomi.duckling

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.analyzer.Analyzer
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.types._

/**
  *
  * @param rawInput
  * @param firstNonAdjacent for a given index 'i' it keeps a first index 'j' greater or equal 'i'
  *     -- such that isAdjacentSeparator (indexable ! j) == False
  *     -- eg. " a document " :: Document
  *     --     firstNonAdjacent = [1,1,3,3,4,5,6,7,8,9,10,12]
  *     -- Note that in this case 12 is the length of the vector, hence not a
  *     -- valid index inside the array, this is intentional.
  */
case class Document(rawInput: String,
                    firstNonAdjacent: Array[Int],
                    lang: LanguageInfo) {

  def isPositionValid(position: Int)(node: Node): Boolean = {
    isAdjacent(position, node.range.start)
  }

  /**
    * True iff a is followed by whitespaces and b.
    *
    * @param a
    * @param b
    * @return
    */
  def isAdjacent(a: Int, b: Int): Boolean = b >= a && firstNonAdjacent(a) >= b

  /**
    * As regexes are matched without whitespace delimitator, we need to check
    * the reasonability of the match to actually be a word.
    *
    * @param start
    * @param end
    * @return
    */
  def isRangeValid(start: Int, end: Int): Boolean = {
    def charClass(c: Char) = {
      if (c.isLower || c.isUpper) 'c'
      else if (c.isDigit) 'd'
      else c
    }

    def isDifferent(a: Char, b: Char) = charClass(a) != charClass(b) || isChinese(a) && isChinese(b)

    start < end &&
    (start == 0 || isDifferent(this ! (start - 1), this ! start)) &&
    (end == length || isDifferent(this ! (end - 1), this ! end))
  }

  def !(i: Int): Char = rawInput.charAt(i)

  def length: Int = rawInput.length

  def stringFromPos(position: Int): (String, (Int, Int) => String, (Int, Int) => (Int, Int)) = {
    val substring = rawInput.substring(position)

    def translateRange(start: Int, len: Int): (Int, Int) =
      (position + start, position + start + len)

    def rangeToText(start: Int, len: Int): String = {
      if (start == -1) ""
      else {
        val (s, t) = translateRange(start, len)
        rawInput.substring(s, t)
      }
    }

    (substring, rangeToText, translateRange)
  }

  def substring(s: Int, t: Int): String = rawInput.substring(s, t)

  def substring(node: Node): String = substring(node.range)

  def substring(range: Range): String = rawInput.substring(range.start, range.end)

  // language info related

  def numTokens: Int = lang.tokens.length

  def tokens: Array[TokenLabel] = lang.tokens

  def dependencyChildren: Map[Int, List[DependencyEdge]] = lang.dependencyChildren

  def word(i: Int): String = lang.tokens(i).word

  def deps(token: TokenLabel): List[DependencyEdge] = lang.dependencyChildren(token.id)

  def findCoveredToken(start: Int, end: Int): Option[TokenLabel] = {
    lang.tokens.find(t => start >= t.start && end <= t.end)
  }

  def adjacentToken(node: Node): Option[TokenLabel] = {
    val Range(s, e) = node.range
    findToken(s, e).flatMap { t =>
        if (lang.numTokens > t.id) token(t.id) else None
    }
  }

  def token(i: Int): TokenLabel = lang.tokens(i)

  def phrase(start: Int, end: Int): String = {
    substring(token(start).start, token(end - 1).end)
  }

  def findToken(start: Int, end: Int): Option[TokenLabel] = lang.tokens.find(t => t.start == start && t.end == end)

  def indexOfToken(offset: Int): Int = lang.tokens.indexWhere(_.start == offset)

  /**
    * 摘自knowledge-base-langs，避免循环依赖
    */
  def isChinese(c: Char): Boolean = {
    val ub = Character.UnicodeBlock.of(c)
    (ub eq Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) || // 1
    (ub eq Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) || // 2
    (ub eq Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) || // 3
    (ub eq Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) || // 4
    (ub eq Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION) || // 5
    (ub eq Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) || // 6
    (ub eq Character.UnicodeBlock.GENERAL_PUNCTUATION) // 7
  }

  override def toString: String = {
    val langsStr = lang.map(", langs: " + _.toString).getOrElse("")
    s"{doc: $rawInput$langsStr}"
  }
}

object Document extends LazyLogging {

  def fromText(rawInput: String, enableAnalyzer: Boolean = false): Document = {
    val lang =
      if (enableAnalyzer) Analyzer.analyze(rawInput)
      else LanguageInfo(rawInput)
    fromLang(lang)
  }

  def fromLang(lang: LanguageInfo): Document = {
    val rawInput = lang.sentence
    val rawInputLength = rawInput.length
    val firstNonAdjacent = scala
      .Range(0, rawInputLength)
      .zip(rawInput)
      .foldRight((rawInputLength, List[Int]()))(gen)
      ._2
      .toArray

    Document(rawInput, firstNonAdjacent, lang)
  }

  private def gen(indexOfElem: (Int, Char), bestOfAcc: (Int, List[Int])): (Int, List[Int]) = {
    val ((ix, elem), (best, acc)) = (indexOfElem, bestOfAcc)
    if (isAdjacentSeparator(elem)) (best, best :: acc)
    else (ix, ix :: acc)
  }

  def isAdjacentSeparator(c: Char): Boolean = List(' ', '\t').contains(c)
}
