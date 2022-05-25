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

package com.xiaomi.duckling.types

case class LanguageInfo(sentence: String,
                        tokens: Array[TokenLabel] = Array(),
                        dependencyChildren: Map[Int, List[DependencyEdge]] = Map()) {

  override def toString: String = {
    val sb = new StringBuffer()
    sb.append(s"Sentence: $sentence {\n")
    val tags = tokens.map(t => "%s".format(t.tag))
    sb.append("  Tokens: %s\n".format(tokens.map(_.word).mkString("[", ", ", "]")))
    sb.append("  POS   : %s\n".format(tags.mkString("[", ", ", "]")))

    val sDeps = dependencyChildren.toList.flatMap {
      case (i, deps) =>
        deps.map { edge =>
          "%s --(%s)-> %s".format(tokens(i - 1).word, edge.label, tokens(edge.modifier - 1).word)
        }
    }
    sb.append("  Deps  : %s\n".format(sDeps.mkString(", ")))
    sb.append("}")
    sb.toString
  }

  def numTokens: Int = tokens.length
}

object LanguageInfo {
  def fromText(s: String, enableAnalyzer: Boolean = false, segItems: Option[Array[TokenLabel]]=None): LanguageInfo = {
    if (enableAnalyzer && segItems.nonEmpty) LanguageInfo(s, segItems.get, Map())
    else LanguageInfo(s)
  }
}

/**
 * 沿用了CoreNLP中的定义
 *
 * @param id 沿用了hanlp CoLLWord中的id，下标从1开始
 * @param word
 * @param start
 * @param end
 * @param tag POS tag
 */
case class TokenLabel(id: Int, word: String, start: Int, end: Int, tag: String)

/**
 *
 * @param label Dependency label
 * @param modifier Position of modifier
 */
case class DependencyEdge(label: String, modifier: Int)