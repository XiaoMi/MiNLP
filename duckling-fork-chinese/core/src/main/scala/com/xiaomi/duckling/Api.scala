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
import java.util
import scala.collection.JavaConverters._
import scala.language.postfixOps
import com.xiaomi.duckling.Types.{Answer, Context, Entity, Options, ResolvedToken, _}
import com.xiaomi.duckling.constraints.TokenSpan
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.engine.Engine._
import com.xiaomi.duckling.ranking.Rank.{rank, resolveAheadByRange}
import com.xiaomi.duckling.ranking.{NaiveBayesRank, Ranker}
import com.xiaomi.duckling.types.{LanguageInfo, Node, TokenLabel}

object Api extends LazyLogging {

  /**
    * Parses `input` and returns a curated list of entities found.
    *
    * @param input      sentence
    * @param context    context
    * @param options    options
    * @param segTokens  segments of sentence
    * @return
    */
  def parseEntities(input: String, context: Context, options: Options, segTokens: Option[Array[TokenLabel]]=None): List[Entity] = {
    val resolvedTokens = analyze(input, context, options, segTokens).map(_.token)
    resolvedTokens.map(formatToken(input, options.entityWithNode))
  }

  /**
    * Returns a curated list of resolved tokens found
    * When `targets` is non-empty, returns only tokens of such dimensions.
    *
    * @param input      sentence
    * @param context    context
    * @param options    options
    * @param segTokens  segments of sentence
    * @return
    */
  def analyze(input: String, context: Context, options: Options, segTokens: Option[Array[TokenLabel]]=None): List[Answer] = {
    analyze(LanguageInfo.fromText(input, options.enableAnalyzer, segTokens), context, options)
  }

  def analyze(lang: LanguageInfo, context: Context, options: Options): List[Answer] = {
    val input = lang.sentence

    val targets = options.targets ++ options.targets.flatMap(_.nonOverlapDims)
    val rules = Rules.rulesFor(context.locale, targets)
    val nodes = parse(rules, lang, options)
    val doc = Document.fromLang(lang)
    // 去掉非目标对象，可能由依赖带入
    val ofTargets =
      if (targets.isEmpty) nodes
      else nodes.filter(t => options.targets.contains(t.token.dim))

    // 只保留完整解析的
    val fullMatchFiltered =
      if (options.full) ofTargets.filter(_.range match {
        case Range(0, l) => l == input.length
        case _           => false
      })
      else ofTargets

    val resolvedTokens = {
      // 在需要做 overlap 组合计算时，需要关闭，Range 的包含并不保证组合上的最优
      if (options.rankOptions.rangeRankAhead) {
        resolveAheadByRange(doc, context, options, fullMatchFiltered)
      } else {
        // 目前只有这一种条件，有额外后续再抽取
        fullMatchFiltered.flatMap(resolveNode(doc, context, options)).filter(TokenSpan.isValid(lang, _))
      }
    }

    // 只保留非latent的
    val latentFiltered =
      if (!options.withLatent) resolvedTokens.filterNot(rt => rt.isLatent)
      else resolvedTokens

    val answers = latentFiltered.map(Answer(input, _))

    // 增加覆盖过滤
    val overlapFiltered = nonOverlap(
      answers.toIndexedSeq,
      options.targets.flatMap(_.nonOverlapDims).diff(options.targets)
    )

    // 排序，相同范围/dim的结果，保留概率最高的
    val ranked = options.rankOptions.ranker match {
      case Some(Ranker.NaiveBayes) =>
        rank(NaiveBayesRank.score, targets, overlapFiltered, options.rankOptions)
      case _ => overlapFiltered
    }

    ranked
  }

  /**
    * for java
    */
  def analyzeJ(input: String, context: Context, options: Options): util.List[Answer] = {
    analyze(input, context, options, None).asJava
  }
  
  def analyzeJ(input: String, context: Context, options: Options, segTokens: Array[TokenLabel]): util.List[Answer] = {
    analyze(input, context, options, Some(segTokens)).asJava
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

  /**
    * 去掉有dim重叠的结果，比如：
    * 第五十五号 => [五十五, 十五号]
    */
  def nonOverlap(a: Seq[Answer], dims: Set[Dimension]): List[Answer] = {
    def overlap(r1: Types.Range, r2: Types.Range): Boolean = {
      r1.start < r2.start && r1.end < r2.end && r1.end > r2.start ||
      r2.start < r1.start && r2.end < r1.end && r2.end > r1.start
    }

    val nonOverlap = (for (i <- a.indices) yield {
      val f1 = a.indices.filter(j => a(i).dim.nonOverlapDims.contains(a(j).dim))

      val jOpt = f1.find { j =>
        overlap(a(i).token.range, a(j).token.range) &&
        a(i).dim.nonOverlapDims.contains(a(j).dim) &&
        a(i).dim.overlap(a(j).token.node.token)
      }

      jOpt match {
        case Some(j) =>
          logger.debug(s"overlap found: ${a(i).text} & ${a(j).text} => drop ${a(i).text}")
          None
        case None => Some(a(i))
      }
    }).flatten.toList

    nonOverlap.filter(a => !dims.contains(a.dim))
  }
}
