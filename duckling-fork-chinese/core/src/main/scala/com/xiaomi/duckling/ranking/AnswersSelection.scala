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
import scalaz.Scalaz._

import com.xiaomi.duckling.Api
import com.xiaomi.duckling.Types.{Answer, Options, RankOptions}
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.numeral.Numeral
import com.xiaomi.duckling.dimension.time.Time
import com.xiaomi.duckling.ranking.Testing.testContext
import com.xiaomi.duckling.ranking.Types.{BagOfFeatures, Datum}

/**
  * 暂时无法投入使用，并不能很好地选出组合结果，但是构造样本的过程还有意义，先留下
  */
object AnswersSelection extends LazyLogging {

  type Answers = List[Answer]

  var classifier = synchronized {
    logger.info("initialize classifier for answer selection ...")
    val data = makeDataset()
    logger.info(s"answer selection: ${data.size}, positive: ${data.count(_._2)}")
    Bayes.train(makeDataset())
  }

  /**
    * 求解区间组合
    * 1. 相同区间的只保留打分最高的
    * 2. 不同区间组合时不能有重叠
    */
  def nonOverlapCombinations(raw: Answers): List[Answers] = {
    // 1. 相同区间的，只保留打分最高的
    implicit val answers =
      raw
        .groupBy(a => (a.dim, a.range))
        .mapValues(_.maxBy(_.score))
        .values
        .toList
        .sortBy(a => (a.range.start, -a.range.end))

    val starts = answers.indices.takeWhile { i =>
      !(0 until i).exists(j => answers(i).range > answers(j).range)
    }.toList

    starts
      .flatMap(i => path(i, List(List(i))))
      .map(is => is.map(answers))
  }

  /**
    * 求不相交的下一个Answer，但是不能有完全跳过一个Answer
    */
  def adjacents(i: Int)(implicit a: Answers): List[Int] = {
    val z = a(i)
    val n = a.length
    (i + 1 until n).find(j => a(j).range > z.range) match {
      case Some(j) =>
        (j until n).takeWhile { k =>
          !(j until k).exists(l => a(k).range > a(l).range)
        }.toList
      case None => Nil
    }
  }

  /**
    * 从当前结点继续寻找下一个有效组合
    */
  def path(i: Int, heads: List[List[Int]])(implicit a: Answers): List[List[Int]] = {
    adjacents(i) match {
      case Nil  => heads
      case list => list.flatMap(j => path(j, heads.map(_ :+ j)))
    }
  }

  /**
    * 去掉"被包含"的情况，比如"十/一"被"十一"完全包含
    */
  def maximizeAnswer(combinations: List[Answers]): List[Answers] = {
    def contain(b: Answers, a: Answers): Boolean = {
      a.forall(an => b.exists(_.range.include(an.range)))
    }

    combinations.filter(c => !combinations.exists(t => t != c && contain(t, c)))
  }

  def makeDataset(): List[Datum] = {
    val examples =
      List(
        ("十一十二", "十一/十二", Numeral),
        ("二十三十", "二十/三十", Numeral),
        ("九九八十一", "九/九/八十一", Numeral),
        ("八月九月", "八月/九月", Time),
        ("八月九月一号", "八月/九月一号", Time),
        ("零一年五月八月", "零一年五月/八月", Time)
      )

    examples.flatMap {
      case (sentence, target, dim) =>
        makeDataset1(sentence, target, dim)
    }
  }

  def makeDataset1(sentence: String, target: String, dim: Dimension): List[Datum] = {
    val options =
      Options(withLatent = false, targets = Set(dim), rankOptions = RankOptions(winnerOnly = false))
    val answers = Api.analyze(sentence, testContext, options)

    // 寻找可能的组合
    val combinations = nonOverlapCombinations(answers)

    // 去掉"被包含"的情况
    val nonContains = maximizeAnswer(combinations)

    val examples = nonContains.map { answers =>
      val label = answer2text(answers) == target
      (extract(answers), label)
    }

    if (!examples.exists(_._2)) {
      sys.error(s"no positive example found, please check [$sentence:$target:${dim.name}]")
    }

    examples
  }

  def answer2text(answers: Answers): String = answers.map(_.text).mkString("/")

  def extract(answers: Answers): BagOfFeatures = {
    answers
      .flatMap(a => a.dim.combinationFeatures(a.token.node))
      .map(f => f.name())
      .groupBy(s => s)
      .fmap(_.size)
  }

  def parse(sentence: String, dim: Dimension) = {
    val options =
      Options(withLatent = false, rankOptions = RankOptions(), targets = Set(dim))
    val answers = Api.analyze(sentence, testContext, options)

    // 寻找可能的组合
    val combinations = nonOverlapCombinations(answers)
    combinations.foreach { a =>
      println(answer2text(a))
    }

    println("no contain")

    // 去掉"被包含"的情况
    val nonContains = maximizeAnswer(combinations)

    nonContains.foreach { a =>
      println(answer2text(a))
    }
    nonContains
  }

  def predict(answers: Answers): Option[Answers] = {
    val candidates = nonOverlapCombinations(answers)
    val nonContains = maximizeAnswer(candidates)
    predictCandidates(nonContains)
  }

  def predictCandidates(candidates: List[Answers]): Option[Answers] = {
    candidates
      .flatMap { answers =>
        val (_class, score) = Bayes.classify(classifier, extract(answers))
        if (_class) Some(answers, score)
        else None
      }
      .maximumBy(_._2)
      .map(_._1)
  }

  def main(args: Array[String]): Unit = {
    val classifier = Bayes.train(makeDataset())

    val sentence = "九九八十一"
    val dim = Numeral

    val candidates = parse(sentence, dim)
    candidates.foreach { answers =>
      println(Bayes.classify(classifier, extract(answers)))
    }

    predictCandidates(candidates) match {
      case Some(answers) => println(answer2text(answers))
      case None          => println("empty")
    }
  }
}
