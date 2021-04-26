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

import com.xiaomi.duckling.ranking.Types.{BagOfFeatures, Class, Datum}

object Bayes {

  case class Classifier(okData: ClassData, koData: ClassData)

  case class ClassData(prior: Double, unseen: Double, likelihoods: Map[String, Double], n: Int)

  /**
    * Compute prior and likelihoods log-probabilities for one class.
    *
    * @param feats
    * @param total
    * @param classTotal
    * @param vocSize
    * @return
    */
  def makeClass(feats: BagOfFeatures, total: Int, classTotal: Int, vocSize: Int): ClassData = {
    val prior = math.log(1.0 * (classTotal + 1e-9) / total)
    val denum = vocSize + feats.values.sum
    val unseen = math.log(1.0 / (denum + 1.0))
    val likelihoods = feats.mapValues(x => math.log((x + 1.0) / denum))

    ClassData(prior, unseen, likelihoods, classTotal)
  }

  /**
    * Train a classifier for a single rule
    *
    * @param datums (Map[Feature, Int], Class)
    * @return
    */
  def train(datums: List[Datum]): Classifier = {
    val total = datums.length
    val (ok, ko) = datums.partition(_._2)

    def merge(xs: List[BagOfFeatures], m: BagOfFeatures): BagOfFeatures =
      xs.foldLeft(m) { case (z: BagOfFeatures, b) => z ++ b }

    val okCounts = merge(ok.map(_._1), Map())
    val koCounts = merge(ko.map(_._1), Map())
    val vocSize = (okCounts ++ koCounts).size
    val okClass = makeClass(okCounts, total, ok.length, vocSize)
    val koClass = makeClass(koCounts, total, ko.length, vocSize)
    Classifier(okData = okClass, koData = koClass)
  }

  def classify(classifier: Classifier, feats: BagOfFeatures): (Class, Double) = {
    val Classifier(okData, koData) = classifier
    val okScore = prob(feats, okData)
    val koScore = prob(feats, koData)
    if (okScore >= koScore) (true, okScore)
    else (false, koScore)
  }

  def prob(feats: BagOfFeatures, classData: ClassData): Double = {
    val ClassData(prior, unseen, likelihoods, _) = classData
    prior + feats.foldRight(0.0) {
      case ((feat, x), res) => res + x * likelihoods.getOrElse(feat, unseen)
    }
  }
}
