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

package com.xiaomi.duckling.dimension.time

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types.{Predicate => _, _}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.TimeDatePredicate
import com.xiaomi.duckling.dimension.time.Time.ruleSequence
import com.xiaomi.duckling.engine.Engine.{produce, Match}
import com.xiaomi.duckling.types.Node

object Prods extends LazyLogging {

  def tt(timeData: TimeData): Token = Token(Time, timeData)

  def tt(opt: Option[TimeData]): Option[Token] = opt.map(td => Token(Time, td))

  val tokenize = (timeData: TimeData) => Token(Time, timeData)

  val intersectDOM: (TimeData, Token) => Option[TimeData] = (td: TimeData, token: Token) => {
    getIntValue(token).flatMap { n =>
      val dayTd =
        TimeData(timePred = TimeDatePredicate(dayOfMonth = n.toInt), timeGrain = Grain.Day)
      intersect(dayTd, td)
    }
  }

  /**
   * 对于应用sequence的，记录左右边界，在生成中间结果时就进行剪枝
   *
   * @param _matches
   * @param options
   * @return
   */
  def limitedSequenceByRange(_matches: List[Match], heads: java.util.Set[Int], options: Options): List[Node] = {
    if (heads.isEmpty) { // 由于解析是自底向上的，所以只需要记录一次
      val _tails = _matches.flatMap {
        case _match@(rule, end, nodes) if rule.name == ruleSequence.name =>
          Some (nodes.head.range, nodes.last.range) // 倒序，先匹配上的在后面
        case _ => None
      }.toMap
      if (_tails.nonEmpty) {
        logger.debug(_tails.map { case (t, h) => "[%02d,%02d)-[%02d,%02d)".format(h.start, h.end, t.start, t.end) }.toList.sorted.mkString("|"))
        // 去掉head在tail中的
        _tails.foreach { case r@(t, h) =>
          if (!_tails.contains(h)) heads.add(h.start)
        }
        logger.debug(s"valid heads $heads")
      }
    }
    _matches.flatMap { case _match@(rule, end, nodes) =>
      if (rule.name == ruleSequence.name) {
        val t = nodes.last.range.start
        if (!heads.contains(t)) None
        else produce(options)(_match)
      } else produce(options)(_match)
    }.distinct
  }
}
