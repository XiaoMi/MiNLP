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
import com.xiaomi.duckling.dimension.matcher.{Varchar, VarcharMatch}
import com.xiaomi.duckling.types.Node

object VarcharLookup {

  def lookupVarLength(doc: Document,
                      lower: Int,
                      upper: Int,
                      position: Int,
                      excludes: List[Regex]): List[Node] = {
    if (position > doc.length) Nil
    else lookupVar(doc, lower, upper, position, excludes)
  }

  def lookupVar(doc: Document,
                lower: Int,
                upper: Int,
                position: Int,
                excludes: List[Regex]): List[Node] = {
    val (substring, rangeToText, translateRange) = doc.stringFromPos(position)

    def f(startLen: (Int, Int)): Option[Node] = startLen match {
      case (0, 0) => None
      // Haskell中的Regex库返回第一个元素为整个匹配项
      case (bsStart, bsLen) =>
        val text = rangeToText(bsStart, bsLen)
        if (excludes.exists(_.findFirstMatchIn(text).nonEmpty)) {
          None
        } else {
          val (start, end) = translateRange(bsStart, bsLen)
          if (doc.isRangeValid(start, end)) {
            val node = Node(
              range = Range(start, end),
              token = Token(VarcharMatch, Varchar(text, excludes)),
              children = Nil,
              rule = None,
              production = null
            )
            Some(node)
          } else None
        }
    }

    val l = substring.length
    if (l < lower) Nil
    else {
      val list = (lower to math.min(l, upper)).flatMap(n => (0 to l - n).map((_, n)))
      list.flatMap(f).toList
    }
  }

  def endsVarcharExpansion(doc: Document, node: Node): Node = {
    if (node.children.isEmpty) node
    else {
      val (updateLeft, leftNeedUpdate) = leftVarcharExpansion(doc, node)
      val (updated, rightNeedUpdate) = rightVarcharExpansion(doc, updateLeft)
      if (leftNeedUpdate || rightNeedUpdate) updateNode(updated)
      else updated
    }
  }

  def updateNode(node: Node): Node = {
    val prod = node.production(node.children.map(_.token))
    node.copy(
      token = prod.get,
      range = Range(node.children.head.range.start, node.children.last.range.end)
    )
  }

  def leftVarcharExpansion(doc: Document, node: Node): (Node, Boolean) = {
    if (node.children.isEmpty || node.range.start == 0) (node, false)
    else {
      val head @ Node(Range(s, t), token, children, _, _, _) = node.children.head
      val (child, updated) =
        token match {
          case Token(VarcharMatch, v @ Varchar(vs, _)) =>
            val start =
              (s - 1 to 0 by -1).find(i => !v.isInvalid(doc.substring(i, t))).map(_ + 1).getOrElse(0)
            val range = head.range.copy(start = start)
            val u =
              head.copy(range = range, token = Token(VarcharMatch, Varchar(doc.substring(range))))
            (u, true)
          case _ =>
            if (children.nonEmpty) {
              val (n, update) = leftVarcharExpansion(doc, head)
              if (update) (updateNode(n), true) else (head, false)
            } else (head, false)
        }
      if (updated) {
        val copied =
          node.copy(range = node.range.copy(start = 0), children = node.children.updated(0, child))
        (copied, true)
      } else (node, false)
    }
  }

  def rightVarcharExpansion(doc: Document, node: Node): (Node, Boolean) = {
    if (node.children.isEmpty || node.range.end == doc.length) (node, false)
    else {
      val last @ Node(Range(s, t), token, children, _, _, _) = node.children.last
      val (child, updated) =
        token match {
          case Token(VarcharMatch, v @ Varchar(vs, _)) =>
            val end =
              (t to doc.length)
                .find(i => v.isInvalid(doc.substring(s, i)))
                .map(_ - 1)
                .getOrElse(doc.length)
            val range = last.range.copy(end = end)
            val u =
              last.copy(range = range, token = Token(VarcharMatch, Varchar(doc.substring(range))))
            (u, true)
          case _ =>
            if (children.nonEmpty) {
              val (n, update) = rightVarcharExpansion(doc, last)
              if (update) { (updateNode(n), true) } else (n, false)
            } else (last, false)
        }
      if (updated) {
        val copied = node.copy(
          range = node.range.copy(end = doc.length),
          children = node.children.updated(node.children.size - 1, child)
        )
        (copied, true)
      } else (node, false)
    }
  }
}
