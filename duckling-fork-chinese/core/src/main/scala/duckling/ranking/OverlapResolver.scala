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

package duckling.ranking

import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging

import scalaz.Scalaz._

import duckling.Types.{Answer, Rule, Token}
import duckling.dimension.Dimension
import duckling.dimension.implicits._
import duckling.dimension.numeral.{Numeral, NumeralValue}
import duckling.dimension.time.{Time, TimeData}
import duckling.dimension.time.enums.Grain
import duckling.types.Node

object OverlapResolver extends LazyLogging {

  /**
    * 解决[九月三月] => 九月三/三月 有重叠的问题
    */
  def solveConflictOfSameDimension(winners: List[Answer],
                                   answers: Map[Dimension, List[Answer]]): List[Answer] = {
    if (winners.size >= 2) {
      winners
        .groupBy(_.dim)
        .flatMap {
          case (dim, values) =>
            if (values.size >= 2) {
              val buf = mutable.Buffer[Answer]()
              values.sliding(2).foreach {
                case p :: q :: _ =>
                  val a = cross(dim, p, buf.lastOption, q, answers)
                  // 五百六五百六 => 五百六/六五/五百六 => 五百六/五/五百六
                  // 五应该被去掉
                  val rangeCovered = buf.lastOption.exists(_.range.include(a.range)) ||
                    q.range.include(a.range)
                  if (!rangeCovered || p.range == q.range) buf += a
                case _ =>
              }
              // TODO 现在只做了sliding判断左右交叉时，左应该如何处理，最后一个应该与入列的结果的最后一个进行比较
              buf += cross(dim, values.last, buf.lastOption, None, answers)
              buf.toList
            } else values
        }
        .toList
        .sortBy(a => a.range)
    } else winners
  }

  def isCross(a: Answer, b: Answer): Boolean = {
    a.range.end > b.range.start
  }

  def cross(dim: Dimension,
            current: Answer,
            before: Option[Answer],
            after: Option[Answer],
            answers: Map[Dimension, List[Answer]]): Answer = {
    val p1 =
      if (before.nonEmpty && before.get.range != current.range && isCross(before.get, current)) {
        dim match {
          case Numeral => numeralCrossBackward(current, before.get, answers(dim))
          case _       => current
        }
      } else current
    if (after.nonEmpty && after.get.range != p1.range && isCross(p1, after.get)) {
      dim match {
        case Time    => timeCrossForward(p1, after.get, answers(dim))
        case Numeral => numeralCrossForward(p1, after.get, answers(dim))
        case _       => p1
      }
    } else p1
  }

  def rangeMaximum(answers: List[Answer], start: Int, end: Int): Option[Answer] = {
    answers
      .filter(t => t.range.start == start && t.range.end == end)
      .maximumBy(_.score)
  }

  def crossBackward(answers: List[Answer],
                    current: Answer,
                    before: Answer,
                    start: Int,
                    end: Int): Answer = {
    rangeMaximum(answers, start, end) match {
      case Some(max) =>
        logger.info(
          s"numeral cross: ${current.sentence} => ${before.text}/${current.text} => ${before.text}/${max.text}"
        )
        max
      case None =>
        logger.error(s"[${current.dim.name}] empty range result found for: ${current.sentence}")
        current
    }
  }

  def crossForward(answers: List[Answer],
                   current: Answer,
                   after: Answer,
                   start: Int,
                   end: Int): Answer = {
    rangeMaximum(answers, start, end) match {
      case Some(max) =>
        logger.info(
          s"numeral cross: ${current.sentence} => ${current.text}/${after.text} => ${max.text}/${after.text}"
        )
        max
      case None =>
        logger.error(s"[${current.dim.name}] empty range result found for: ${current.sentence}")
        current
    }
  }

  def isRule(a: Answer, rule: Rule): Boolean = {
    a.token.node.rule.contains(rule.name)
  }

  def timeCrossForward(a: Answer, b: Answer, answers: List[Answer]): Answer = {
    // 问题： 十月三月 => 十月三/三月
    namedMonthWithoutDayUnit(a.token.node, "date: <named-month> <day-of-month>") match {
      case Some(node) =>
        crossForward(answers, a, b, a.range.start, node.children(0).range.end)
      case _ => a
    }
  }

  /**
    * 求应用规则的最右Node
    */
  def namedMonthWithoutDayUnit(node: Node, rule: String): Option[Node] = {
    if (node.rule.contains(rule)) Some(node)
    else {
      node.children.flatMap(n => namedMonthWithoutDayUnit(n, rule)).maximumBy(_.range.end)
    }
  }

  def grain(a: Answer): Option[Grain] = {
    a.token.node.token match {
      case Token(Time, td: TimeData) => Some(td.timeGrain)
      case _                         => None
    }
  }

  def numeralCrossForward(current: Answer, after: Answer, answers: List[Answer]): Answer = {
    // 三十四百 => 三十四/四百 => 三十/四百
    val case1 = isRule(current, Numeral.ruleNumeralsIntersectConsecutiveUnit) &&
      isRule(after, Numeral.ruleMultiplys)
    // 三十三百四十 => 三十三/三百四十 => 三十/三百四十
    val case2 = isRule(current, Numeral.ruleNumeralsIntersectConsecutiveUnit) &&
      isRule(after, Numeral.ruleNumeralsIntersectConsecutiveUnit)
    // 五三百六 => 五三/三百六 => 五/三百六
    val case3 = isRule(current, Numeral.ruleCnSequence)
    // 十一十二 保持前面的不变
    if (case2 && numeralValue(current) < 100 && numeralValue(after) < 100) current
    else if (case1 || case2 || case3) {
      crossForward(answers, current, after, current.range.start, after.range.start)
    } else current
  }

  def numeralCrossBackward(current: Answer, before: Answer, answers: List[Answer]): Answer = {
    // 十二十三 => 十二/二十三 => 十二/十三
    val case1 = isRule(before, Numeral.ruleNumeralsIntersectConsecutiveUnit) &&
      isRule(current, Numeral.ruleNumeralsIntersectConsecutiveUnit)
    // 二十六点二十 => 二十六点二/二十 => 二十六点二/十
    val case2 = isRule(before, Numeral.ruleDecimalCharNumeral) &&
      isRule(current, Numeral.ruleMultiplys)
    // 二百五三
    val case3 = isRule(current, Numeral.ruleCnSequence)
    if (case1 || case2 || case3) {
      crossBackward(answers, current, before, before.range.end, current.range.end)
    } else current
  }

  def numeralValue(a: Answer): Double = {
    a.token.value.asInstanceOf[NumeralValue].n
  }

}
