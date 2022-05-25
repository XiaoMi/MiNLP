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

import java.time.{ZoneId, ZonedDateTime}
import java.util.Locale
import scala.collection.JavaConverters._
import scala.util.matching.Regex
import com.typesafe.config.ConfigFactory

import com.xiaomi.duckling.dimension.{CorpusSets, Dimension, EnumeratedDimension}
import com.xiaomi.duckling.dimension.numeral.NumeralOptions
import com.xiaomi.duckling.dimension.time.TimeOptions
import com.xiaomi.duckling.dimension.time.Types.DuckDateTime
import com.xiaomi.duckling.engine.LexiconLookup.Dict
import com.xiaomi.duckling.engine.PhraseLookup.PhraseMatcherFn
import com.xiaomi.duckling.ranking.Ranker
import com.xiaomi.duckling.ranking.Types.Feature
import com.xiaomi.duckling.types.Node

object Types {

  val conf = ConfigFactory.load().getConfig("duckling.core")

  type Pattern = List[PatternItem]
  type Production = PartialFunction[(Options, List[Token]), Option[Token]]
  type TokensProduction = PartialFunction[List[Token], Option[Token]]
  type Predicate = PartialFunction[Token, Boolean]
  type Extraction = PartialFunction[(Document, List[Node]), List[Feature]]

  val EQ = 0
  val LT = -1
  val GT = 1

  val ZoneCN = ZoneId.of("Asia/Shanghai")

  def tokens(tp: TokensProduction): Production = {
    case (_: Options, _tokens: List[Token]) if tp.isDefinedAt(_tokens) => tp(_tokens)
    case _ => None
  }

  // Predicate helpers
  def isDimension(dim: Dimension*): Predicate = {
    case token: Token => dim.contains(token.dim)
  }
  val emptyProduction: Production = {
    case _ => None
  }
  val emptyExtraction: Extraction = {
    case (_, _) => Nil
  }
  val emptyPredicate: Predicate = {
    case _: Token => false
  }

  def and(ps: Predicate*): Predicate = {
    case token: Token =>
      ps.forall(p => p.isDefinedAt(token) && p(token))
  }

  def or(ps: Predicate*): Predicate = {
    case token: Token =>
      ps.exists(p => p.isDefinedAt(token) && p(token))
  }

  def not(p: Predicate): Predicate = {
    case token: Token => !(p.isDefinedAt(token) && p(token))
  }

  trait Resolvable {

    /**
      * Dimension -> Dimension Data -> Output Value
      * 后两步可以合并
      *
      * @param context
      * @param options
      * @return (value, isLatent)
      */
    def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)]
  }

  trait ResolvedValue {
    def schema: Option[String] = None
  }

  trait NumeralValue extends ResolvedValue {
    val n: Double
  }

  trait PatternItem {
    def predicate(token: Token): Boolean
  }

  case class Token(dim: Dimension, data: Resolvable) {
    override def toString: String = s"{dim = $dim, data = $data}"
  }

  /**
    * 暂未使用
    *
    * @param referenceTime
    * @param locale
    */
  case class Context(referenceTime: ZonedDateTime, locale: Locale) {

    val datetime: DuckDateTime = new DuckDateTime(referenceTime)


    /**
      * for java
      */
    def this() = {
      this(ZonedDateTime.now(ZoneCN), Locale.CHINA)
    }
  }

  /**
    * 不使用 scala.Range，避免不能pattern matching的情况
    *
    * @param start
    * @param end
    */
  case class Range(start: Int, end: Int) {

    def rangeEq(s: Int, t: Int): Boolean = start == s && end == t

    def length: Int = end - start

    def >(o: Range): Boolean = start >= o.end

    def <(o: Range): Boolean = end <= o.start

    def include(o: Range): Boolean = start <= o.start && end >= o.end

    override def toString: String = s"[$start, $end)"
  }

  case class ItemRegex(regex: Regex) extends PatternItem {
    override def predicate(token: Token): Boolean = true
  }

  case class ItemPredicate(f: Predicate) extends PatternItem {
    override def predicate(token: Token): Boolean = (f orElse emptyPredicate)(token)

    override def toString: String = "predicate = (Token => Bool)"
  }

  val DefaultExcludes = List("^\\s".r, "\\s$".r)

  case class ItemVarchar(lower: Int, upper: Int, excludes: List[Regex] = DefaultExcludes)
      extends PatternItem {
    override def predicate(token: Token): Boolean = true
  }

  case class ItemPhrase(mf: PhraseMatcherFn, min: Int, max: Int) extends PatternItem {
    override def predicate(token: Token): Boolean = true
  }

  case class ItemLexicon(dict: Dict) extends PatternItem {
    override def predicate(token: Token): Boolean = true
  }

  /**
    * 传递参数的对象
    *
    * @param withLatent latent结果是否返回
    * @param full       为true时，只返回完整匹配整串的结果
    */
  case class Options(withLatent: Boolean = true,
                     full: Boolean = false,
                     debug: Boolean = false,
                     targets: Set[Dimension] = Set(),
                     varcharExpand: Boolean = false,
                     entityWithNode: Boolean = false,
                     rankOptions: RankOptions = RankOptions(),
                     timeOptions: TimeOptions = TimeOptions(),
                     numeralOptions: NumeralOptions = NumeralOptions()) {

    def enableAnalyzer: Boolean = {
      targets.flatMap(Dimension.dimDependents).exists(_.enableAnalyzer)
    }

    /**
      * for java
      */
    def this(targets: java.util.Set[String], withLatent: Boolean) = {
      this(
        withLatent = withLatent,
        rankOptions = RankOptions(ranker = Some(Ranker.NaiveBayes), combinationRank = true),
        full = false,
        debug = false,
        targets = targets.asScala.map(CorpusSets.namedDimensions).toSet,
        varcharExpand = false,
        entityWithNode = false
      )
    }

    // 都是Set会被认为与前一个冲突
    def this(targets: java.util.List[EnumeratedDimension], withLatent: Boolean) = {
      this(
        withLatent = withLatent,
        rankOptions = RankOptions(ranker = Some(Ranker.NaiveBayes)),
        full = false,
        debug = false,
        targets = targets.asScala.map(_.getDimension).toSet,
        varcharExpand = false,
        entityWithNode = false
      )
    }

    def withTimeOptions(timeOptions: TimeOptions): Options = {
      copy(timeOptions = timeOptions)
    }

    def withNumeralOptions(numeralOptions: NumeralOptions): Options = {
      copy(numeralOptions = numeralOptions)
    }

    def withRankOptions(rankOptions: RankOptions): Options = {
      copy(rankOptions = rankOptions)
    }
  }

  /**
    * 排序的选项
    *
    * @param winnerOnly      是否只保留分数最高的结果
    * @param ranker          排序使用的分类器
    * @param combinationRank 组合排序
    * @param rangeRankAhead  先进行范围排序，再做打分。可以减少节点进入 resolved 阶段，提升效率
    */
  case class RankOptions(winnerOnly: Boolean = true,
                         ranker: Option[Ranker] = Some(Ranker.NaiveBayes),
                         combinationRank: Boolean = false,
                         rangeRankAhead: Boolean = false)

  case class Rule(name: String,
                  pattern: Pattern,
                  prod: Production,
                  features: Extraction = emptyExtraction)

  case class ResolvedToken(range: Range, node: Node, value: ResolvedValue, isLatent: Boolean) {
    override def toString: String = {
      s"{range = [${range.start},${range.end}), node = $Node, rval = $value, latent = $isLatent}"
    }
  }

  case class ResolvedVal(dimension: Dimension, value: ResolvedValue)

  case class Entity(dim: String,
                    body: String,
                    value: ResolvedValue,
                    start: Int,
                    end: Int,
                    latent: Boolean,
                    enode: Option[Node] = None)

  case class Answer(sentence: String,
                    token: ResolvedToken,
                    features: List[Feature] = Nil,
                    score: Double = -1.0) {

    val dim: Dimension = token.node.token.dim

    val text: String = {
      val Range(s, t) = token.range
      sentence.substring(s, t)
    }

    val range: Range = token.range

    def composeFeatures: List[Feature] = {
      dim.combinationFeatures(token.node)
    }

    override def toString: String = {
      s"{$text, ${token.range}, value = ${token.value}, score = $score}"
    }
  }
}
