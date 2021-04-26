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

import com.google.common.cache.CacheBuilder
import com.typesafe.scalalogging.LazyLogging

import java.util
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.language.postfixOps

import com.xiaomi.duckling.Types.{Answer, Context, Entity, Options, ResolvedToken, _}
import com.xiaomi.duckling.constraints.TokenSpan
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.engine.Engine._
import com.xiaomi.duckling.ranking.Rank.{rank, resolveAheadByRange}
import com.xiaomi.duckling.ranking.{NaiveBayesRank, Ranker}
import com.xiaomi.duckling.types.{LanguageInfo, Node}

object Api extends LazyLogging {
  lazy private val enableTimeoutCache = conf.getBoolean("timeout.cache.enable") // 开启缓存的开关
  lazy private val queryCntThreshold = conf.getInt("timeout.cache.start.cnt") // 开启缓存的query计数阈值
  lazy private val timeoutCacheDuration = conf.getLong("timeout.cache.duration") // 缓存的时长timeout.cache.cnt
  lazy private val timeoutCacheMaxcnt = conf.getLong("timeout.cache.maxcnt") // 缓存的最大数量
  private var query_cnt = 0 // 当前query计数

  // 缓存超时query
  lazy private val timeoutCache = CacheBuilder
    .newBuilder()
    .maximumSize(timeoutCacheMaxcnt)
    .expireAfterWrite(timeoutCacheDuration, TimeUnit.MINUTES)
    .weakKeys()
    .build[String, String]()


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
    * parse with timeout
    * reture empty list when timeout
    * @param lang    text/tokens/dep
    * @param context  context
    * @param options  options
    * @return
    */
  private def analyzeWithTimeout(lang: LanguageInfo,
                                 context: Context,
                                 options: Options): List[Answer] = {
    val input = lang.sentence
    try {
      if (null == timeoutCache.getIfPresent(input)) {
        val future = Future { analyzeWithoutTimeout(lang, context, options) }
        Await.result(future, options.timeout.get milliseconds)
      } else List.empty[Answer]
    } catch {
      case e: TimeoutException =>
        logger.error(s"error when parse query=$input, message:$e")
        if (query_cnt > queryCntThreshold && enableTimeoutCache) timeoutCache.put(input, "timeout")
        List.empty[Answer]
    }
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
    analyze(LanguageInfo.fromText(input, options.enableAnalyzer), context, options)
  }

  def analyze(lang: LanguageInfo, context: Context, options: Options): List[Answer] = {
    if (query_cnt <= queryCntThreshold) query_cnt += 1

    if (options.timeout.exists(_ > 0) && query_cnt > queryCntThreshold) {
      analyzeWithTimeout(lang, context, options)
    } else {
      analyzeWithoutTimeout(lang, context, options)
    }
  }

  def analyzeWithoutTimeout(lang: LanguageInfo, context: Context, options: Options): List[Answer] = {
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
    analyze(input, context, options).asJava
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
