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

package com.xiaomi.duckling.ranking

import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.constraints.TokenSpan
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.engine.Engine.resolveNode
import com.xiaomi.duckling.types.Node

object Rank extends LazyLogging {

  def rank(rankFn: Answer => Answer,
           targets: Set[Dimension],
           answers: List[Answer],
           rankOptions: RankOptions): List[Answer] = {
    val candidates = answers.map(rankFn)
    if (rankOptions.winnerOnly) {
      val toBeConfirmed = winners(candidates)
      if (rankOptions.combinationRank) {
        OverlapResolver.solveConflictOfSameDimension(toBeConfirmed, answers.groupBy(_.dim))
      } else toBeConfirmed
    } else candidates.distinct.sortBy(-_.score)
  }

  def winners(xs: List[Answer]): List[Answer] = {
    val champions = xs.filter(x => xs.forall(y => AnswerOrdering.compare(x, y) != LT))
    logger.debug(s"choose winner [${champions.size}] out of [${xs.size}]")
    champions
  }

  /**
   * 对 组合的节点 [Node]，先按 Range 进行排序，相同 Range 的需要保留，再进行打分上的排序
   * @param doc
   * @param context
   * @param options
   * @param nodes
   * @return
   */
  def resolveAheadByRange(doc: Document,
                          context: Context,
                          options: Options,
                          nodes: List[Node]): List[ResolvedToken] = {
    val stash = mutable.Buffer[ResolvedToken]()
    var nResolve = 0
    var total = 0
    nodes.sorted(NodeOrdering).foreach { node =>
      val noBigger = !stash.exists(y => NodeOrdering.compare(node, y.node) == GT)
      total += 1
      if (noBigger) {
        nResolve += 1
        resolveNode(doc, context, options)(node) match {
          // 同外层，目前只有这一种条件，有额外后续再抽取
          case Some(rt) if TokenSpan.isValid(doc.lang, rt)  =>
            if (noBigger) stash.append(rt)
          case None =>
        }
      }
    }
    logger.debug(s"resolve ahead: $total => $nResolve")
    stash.toList
  }

  object RangeOrdering extends Ordering[Range] {
    override def compare(x: Range, y: Range): Int = {
      val Range(s1, e1) = x
      val Range(s2, e2) = y

      val starts = s1.compare(s2)
      val ends = e1.compare(e2)

      starts match {
        case EQ => ends
        case LT =>
          ends match {
            case LT => EQ
            case _  => GT
          }
        case GT =>
          ends match {
            case GT => EQ
            case _  => LT
          }
      }
    }
  }

  object NodeOrdering extends Ordering[Node] {
    override def compare(x: Node, y: Node): Int = {
      -RangeOrdering.compare(x.range, y.range)
    }
  }

  object AnswerOrdering extends Ordering[Answer] {
    override def compare(x: Answer, y: Answer): Int = {
      val r1 = x.token.range
      val r2 = y.token.range

      RangeOrdering.compare(r1, r2) match {
        case EQ if r1 == r2 && x.dim == y.dim =>
          if (math.abs(x.score - y.score) < 1e-5) EQ else x.score.compare(y.score)
        case z => z
      }
    }
  }
}
