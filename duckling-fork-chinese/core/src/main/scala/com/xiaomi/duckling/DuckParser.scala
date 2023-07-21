package com.xiaomi.duckling
import java.util.{Set => JSet}
import java.util

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.dimension.{Dimension, FullDimensions}
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.constraints.TokenSpan
import com.xiaomi.duckling.engine.Engine.{parse, resolveNode}
import com.xiaomi.duckling.ranking.{NaiveBayesRank, Ranker}
import com.xiaomi.duckling.ranking.Rank.{rank, resolveAheadByRange}
import com.xiaomi.duckling.types.LanguageInfo

class DuckParser(dimensions: Set[String], modelResource: String = "naive_bayes.kryo") extends LazyLogging {

  def this(dimensions: JSet[String]) = {
    this(dimensions.asScala.toSet)
  }

  private val dims: Set[Dimension] = FullDimensions.convert(dimensions)
  val ranker = new NaiveBayesRank(modelResource)

  /**
   * Returns a curated list of resolved tokens found
   * When `targets` is non-empty, returns only tokens of such dimensions.
   *
   * @param lang
   * @param context
   * @param options
   * @return
   */
  def analyze(lang: LanguageInfo, context: Context, options: Options): List[Answer] = {
    val input = lang.sentence

    val _targets = options.targets.intersect(dims)
    if (_targets.size != options.targets.size) {
      logger.warn(s"targets: ${options.targets} prune to ${_targets}")
    }
    val targets = _targets ++ _targets.flatMap(_.nonOverlapDims)

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
        case _ => false
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
        rank(ranker.score, targets, overlapFiltered, options.rankOptions)
      case _ => overlapFiltered
    }

    ranked
  }

  /**
   * for scala
   */
  def analyze(input: String, context: Context, options: Options): List[Answer] = {
    analyze(LanguageInfo.fromText(input, options.enableAnalyzer), context, options)
  }

  /**
   * for java
   */
  def analyzeJ(input: String, context: Context, options: Options): util.List[Answer] = {
    analyze(input, context, options).asJava
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

object DuckParser {

}