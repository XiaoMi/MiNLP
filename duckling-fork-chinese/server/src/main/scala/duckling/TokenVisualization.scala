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

package duckling

import org.apache.commons.text.StringEscapeUtils

import duckling.Types.Answer
import duckling.dimension.time.TimeValue
import duckling.dimension.time.Types.{IntervalValue, SimpleValue}
import duckling.WebServer.template
import duckling.dimension.time.duration.DurationData
import duckling.types.Node

object TokenVisualization {

  val tableTemplate =
    """
      |<div class="tried-container" data-reactid=".2.2">
      |    <div data-reactid=".2.2.0">
      |        <h3 class="tagged" data-reactid=".2.2.0.0">
      |            <span data-reactid=".2.2.0.0.0">${query_before_highlight}</span>
      |            <span class="t-highl" data-reactid=".2.2.0.0.1">${query_highlight}</span>
      |            <span data-reactid=".2.2.0.0.2">${query_after_highlight}</span>
      |        </h3>
      |        <div class="value" data-reactid=".2.2.0.1"><span class="start" data-reactid=".2.2.0.1.0">${value}</span><span
      |                class="grain" data-reactid=".2.2.0.1.1">${grain}</span></div>
      |        ${tokens}
      |    </div>
      |</div>
      |""".stripMargin

  def encode(s: String) = StringEscapeUtils.escapeHtml4(s)

  def toTable(query: String, t: Int)(node: Node): String = {
    val text = if (node.rule.isEmpty) {
      s"""<div class="text">${encode(query.substring(node.range.start, node.range.end))}</div>"""
    } else ""
    val list = node.children.map(toTable(query, t + 2))
    val children =
      if (list.nonEmpty) list.mkString(s"<div>", "\n", "</div>")
      else ""

    val lines = s"""<div class="token">
                   |  $children
                   |  $text
                   |  <div class="rule">${encode(node.rule.getOrElse(""))}</div>
                   |</div>""".stripMargin
    lines.split("\n").map(" " * t + _).mkString("\n")
  }

  def answerToTable(answer: Answer): String = {
    val sentence = answer.sentence
    val value = answer.token.value.toString

    val grain = answer.token.value match {
      case TimeValue(timeValue, _, _, _, _) =>
        timeValue match {
          case IntervalValue(start, _) => start.grain.name()
          case SimpleValue(instant) => instant.grain.name()
          case _ => ""
        }
      case DurationData(_, grain, _) => grain.name()
      case _ => ""
    }
    val node = answer.token.node
    val tokens = toTable(sentence, 8)(node)

    val queryHighlight = sentence.substring(node.range.start, node.range.end)
    val queryBeforeHighlight = sentence.substring(0, node.range.start)
    val queryAfterHighlight = sentence.substring(node.range.end)

    tableTemplate
      .replace("${query_before_highlight}", queryBeforeHighlight)
      .replace("${query_highlight}", queryHighlight)
      .replace("${query_after_highlight}", queryAfterHighlight)
      .replace("${value}", value)
      .replace("${grain}", grain)
      .replace("${tokens}", tokens)
  }

  def toHtml(query: String, answers: List[Answer]): String = {
    val tables =
      if (answers.nonEmpty) answers.map(answerToTable).mkString("\n")
      else {
        tableTemplate
          .replace("${query_before_highlight}", query)
          .replace("${query_highlight}", "")
          .replace("${query_after_highlight}", "")
          .replace("${value}", "")
          .replace("${grain}", "")
          .replace("${tokens}", "")
      }
    template.replace("${tables}", tables)
  }
}
