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

package com.xiaomi.duckling.dimension.time.duration

import scalaz.std.string.parseInt
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.regexMatch
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.numeral.seq.{DigitSequence, DigitSequenceData, isDigitLeading0, isDigitLengthGt, isDigitOfWidth}
import com.xiaomi.duckling.dimension.numeral.{Numeral, NumeralData}
import com.xiaomi.duckling.dimension.time.GrainWrapper
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.grain.{GrainData, TimeGrain}
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.unitNumber.UnitNumber

trait Rules extends DimRules {
  val fuzzyOn: Boolean = conf.getBoolean("dimension.time.fuzzy.on")
  val fuzzyValue: Int = conf.getInt("dimension.time.fuzzy.value")

  def compatibleWithUnitNumber(g: Grain): Boolean = {
    g match {
      case Grain.Hour    => true
      case Grain.Week    => true
      case Grain.Month   => true
      case Grain.Quarter => true
      case _             => false
    }
  }

  val ruleIntegerUnitOfDuration = Rule(
    name = "<integer> <unit-of-duration>",
    pattern = List(
      and(
        isNatural,
        isNumberOrUnitNumber,
        not(isDigitLengthGt(1)),
        // 一九八七 年 不召回
        not(isCnSequence)
      ).predicate,
      isDimension(TimeGrain).predicate
    ),
    prod = tokens {
      case t1 :: Token(TimeGrain, GrainData(g, latent)) :: _ =>
        if (isDimension(UnitNumber)(t1) && !compatibleWithUnitNumber(g)) None
        else {
          if (isDimension(Numeral)(t1) && g == Month) None
          else {
            t1 match {
              case Token(_, NumeralData(v, _, _, seq, _, _)) =>
                val n = math.floor(v).toInt
                // 2012年 不召回, 两千年召回
                if (seq.nonEmpty && g == Year && 1950 <= n && n <= 2050) None
                else Token(Duration, DurationData(n, g, latent = latent))
              case _ => None
            }
          }
        }
    }
  )

  // TODO 这里应该参数化
  val ruleFewDuration = Rule(
    name = "few <unit-of-duration>",
    pattern = List("几个?".regex, isDimension(TimeGrain).predicate),
    prod = tokens {
      case _ :: Token(TimeGrain, GrainData(g, _)) :: _ =>
        if (fuzzyOn) Token(Duration, DurationData(fuzzyValue, g, latent = true, fuzzy = true))
        else None
    }
  )

  val ruleDurationQuarterOfAnHour = Rule(
    name = "quarter of an hour",
    pattern = List(and(isNatural, isNumberOrUnitNumber).predicate, "刻钟?".regex),
    prod = tokens {
      case t1 :: _ =>
        for (i <- getIntValue(t1)) yield tt(15 * i.toInt, Minute)
    }
  )

  val ruleDurationDotNumeralHours = Rule(
    name = "number.number hours",
    pattern = List("(\\d+)\\.(\\d+) *小时".regex),
    prod = regexMatch {
      case _ :: h :: m :: _ =>
        for {
          hh <- parseInt(h).toOption
          mm <- parseInt(m).toOption
        } yield {
          val mden = math.pow(10, m.length).toInt
          tt(60 * hh + 60 * mm / mden, Minute)
        }
    }
  )

  val ruleDurationHalfATimeGrain = Rule(
    name = "half a <time-grain>",
    pattern = List("半个?".regex, isDimension(TimeGrain).predicate),
    prod = tokens {
      case _ :: Token(TimeGrain, GrainData(grain, _)) :: _ =>
        for (d <- timesOneAndAHalf(grain, 0)) yield Token(Duration, d)
    }
  )

  val ruleDurationNumberGrainAndHalf = Rule(
    name = "<natural> <unit-of-duration> and a half",
    pattern = List(isNatural.predicate, isDimension(TimeGrain).predicate, "半钟?".regex),
    prod = tokens {
      case t1 :: Token(TimeGrain, GrainData(g, _)) :: _ =>
        for {
          i <- getIntValue(t1)
          d <- timesOneAndAHalf(g, i.toInt)
        } yield Token(Duration, d)
    }
  )

  val ruleDurationNumberHalfGrain = Rule(
    name = "<natural> half <unit-of-duration>",
    pattern = List(isDimension(UnitNumber).predicate, "半".regex, isDimension(TimeGrain).predicate),
    prod = tokens {
      case t1 :: _ :: Token(TimeGrain, GrainData(g, _)) :: _ =>
        for {
          i <- getIntValue(t1)
          d <- timesOneAndAHalf(g, i.toInt)
        } yield Token(Duration, d)
    }
  )

  val ruleCompositeDuration = Rule(
    name = "composite <duration> <duration>",
    pattern =
      List(isNatural.predicate, isDimension(TimeGrain).predicate, isDimension(Duration).predicate),
    prod = tokens {
      case t1 :: Token(TimeGrain, GrainData(g, _)) :: Token(_, dd@DurationData(_, dg, _, _)) :: _
        if g > dg && g != Month =>
        for (i <- getIntValue(t1)) yield Token(Duration, DurationData(i.toInt, g) + dd)
    }
  )

  /**
    * 两年零三个月
    */
  val ruleCompositeDuration2 = Rule(
    name = "composite <duration> 零 <duration>",
    pattern = List(
      isNatural.predicate,
      isDimension(TimeGrain).predicate,
      "(零|外加|加上|加)".regex,
      isNotLatentDuration.predicate
    ),
    prod = tokens {
      case t1 :: Token(TimeGrain, GrainData(g, _)) :: _ :: Token(_, dd@DurationData(_, dg, _, _)) :: _
        if g > dg && g != Month =>
        for (i <- getIntValue(t1)) yield Token(Duration, DurationData(i.toInt, g) + dd)
    }
  )

  /**
    * 1分09秒/1小时09分
    */
  val ruleCompositeDuration3 = Rule(
    name = "composite <duration> 0 <duration>",
    pattern = List(
      isNatural.predicate,
      isDimension(TimeGrain).predicate,
      and(
        isDigitOfWidth(2),
        isDigitLeading0
      ).predicate,
      isDimension(TimeGrain).predicate
    ),
    prod = tokens {
      case t1 :: Token(TimeGrain, GrainData(g0, _)) :: Token(DigitSequence, DigitSequenceData(s, _, _)) :: Token(TimeGrain, GrainData(g1, _)) :: _
        if g0 > g1 =>
        for (i <- getIntValue(t1)) yield Token(Duration, DurationData(i.toInt, g0) + DurationData(s.toInt, g1))
    }
  )
}
