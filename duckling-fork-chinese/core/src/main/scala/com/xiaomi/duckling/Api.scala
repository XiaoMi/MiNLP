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

import java.util

import scala.language.postfixOps

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.{Dimension, FullDimensions}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.types.{LanguageInfo, Node}

object Api extends LazyLogging {

  private lazy val parser = new DuckParser(FullDimensions.namedDimensions.keySet)

  /**
    * Parses `input` and returns a curated list of entities found.
    *
    * @param input
    * @param context
    * @param options
    * @return
    */
  def parseEntities(input: String, context: Context, options: Options): List[Entity] = {
    val resolvedTokens = analyze(input, context, options).map(_.token)
    resolvedTokens.map(formatToken(input, options.entityWithNode))
  }
  
  /**
    * Returns a curated list of resolved tokens found
    * When `targets` is non-empty, returns only tokens of such dimensions.
    *
    * @param input
    * @param context
    * @param options
    * @return
    */
  def analyze(input: String, context: Context, options: Options): List[Answer] = {
    parser.analyze(input, context, options)
  }

  def analyze(lang: LanguageInfo, context: Context, options: Options): List[Answer] = {
    parser.analyze(lang, context, options)
  }

  /**
    * for java
    */
  def analyzeJ(input: String, context: Context, options: Options): util.List[Answer] = {
    parser.analyzeJ(input, context, options)
  }

  def formatToken(sentence: String, withNode: Boolean)(resolved: ResolvedToken): Entity = {
    val body = sentence.substring(resolved.range.start, resolved.range.end)
    val ResolvedToken(range, node @ Node(_, Token(dim, _), _, _, _, _), value, isLatent) = resolved
    Entity(
      dim = dim.name,
      body = body,
      value = value,
      start = range.start,
      end = range.end,
      latent = isLatent,
      enode = if (withNode) node else None
    )
  }
}
