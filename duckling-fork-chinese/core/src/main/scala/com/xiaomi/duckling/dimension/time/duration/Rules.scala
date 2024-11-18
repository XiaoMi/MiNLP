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
import com.xiaomi.duckling.dimension.matcher.GroupMatch
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
  def compatibleWithUnitNumber(g: Grain): Boolean = {
    g match {
      case Grain.Hour    => true
      case Grain.Week    => true
      case Grain.Month   => true
      case Grain.Quarter => true
      case _             => false
    }
  }

  val tp: TokensProduction = {
    case t1 :: Token(TimeGrain, GrainData(g, latent, _)) :: _ =>
      if (isDimension(UnitNumber)(t1) && !compatibleWithUnitNumber(g)) None
      else {
        if (isDimension(Numeral)(t1) && g == Month) None
        else {
          t1 match {
            case Token(_, NumeralData(v, _, _, seq, _, _)) =>
              val n = math.floor(v).toInt
              // 2012年 不召回, 两千年召回
              if (seq.nonEmpty && g == Year && 1950 <= n && n <= 2050) None
              else Token(Duration, DurationData(n, g, latent = latent, schema = durationSchema(n.toString, g)))
            case _ => None
          }
        }
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
    prod = tokens(tp)
  )

  val ruleIntegerUnitOfDuration2 = Rule(
    name = "<integer> <unit-of-duration> exactly",
    pattern = List(
      and(
        isNatural,
        isNumberOrUnitNumber,
        not(isDigitLengthGt(1)),
        // 一九八七 年 不召回
        not(isCnSequence)
      ).predicate,
      isDimension(TimeGrain).predicate,
      "整".regex
    ),
    prod = tokens(tp)
  )

  val ruleFewDuration = Rule(
    name = "few <unit-of-duration>",
    pattern = List("几个?".regex, isDimension(TimeGrain).predicate),
    prod = optTokens {
      case (options: Options, _ :: Token(TimeGrain, GrainData(g, _, _)) :: _) if options.timeOptions.durationFuzzyOn =>
        Token(Duration,
          DurationData(
            options.timeOptions.durationFuzzyValue, g,
            latent = true,
            fuzzy = true,
            schema = durationSchema(options.timeOptions.durationFuzzyValue.toString, g)))
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

  val ruleDurationDotNumeral = Rule(
    name = "number.number grain",
    pattern = List(isInteger.predicate, "[点\\.]".regex, or(isDimension(DigitSequence), isIntegerBetween(0, 9)).predicate, isDimension(TimeGrain).predicate),
    prod = tokens {
      case (t1@ Token(_, nd: NumeralData)) :: _ :: Token(_, decimal) :: Token(_, GrainData(g, false, _)) :: _ =>
        val (dOpt, length) = decimal match {
          case ds: DigitSequenceData => (parseInt(ds.seq).toOption, ds.seq.length)
          case nd: NumeralData => (getIntValue(nd.value).map(_.toInt), 1)
        }
        (for {
          i <- getIntValue(t1).map(_.toInt)
          d <- dOpt
        } yield {
          val mden = math.pow(10, length).toInt
          val token: Option[Token] = g match {
            case Grain.NoGrain => None
            case Grain.Second => None
            case Grain.Minute => tt(60 * i + 60 * d / mden, Second)
            case Grain.Hour => tt(60 * i + 60 * d / mden, Minute)
            case Grain.Day => tt(24 * i + 24 * d / mden, Hour)
            case Grain.Week => None
            case Grain.Month => tt(30 * i + 30 * d / mden, Day)
            case Grain.Quarter => None
            case Grain.Year => tt(12 * i + 12 * d / mden, Day)
          }
          token
        }).flatten
    }
  )

  val ruleDurationHalfATimeGrain = Rule(
    name = "half a <time-grain>",
    pattern = List("半个?".regex, isDimension(TimeGrain).predicate),
    prod = tokens {
      case _ :: Token(TimeGrain, GrainData(grain, _, _)) :: _ =>
        for (d <- timesOneAndAHalf(grain, 0)) yield Token(Duration, d)
    }
  )

  val ruleDurationNumberGrainAndHalf = Rule(
    name = "<natural> <unit-of-duration> and a half",
    pattern = List(isNatural.predicate, "(分半钟?|天半|年半)".regex),
    prod = tokens {
      case t1 :: Token(_, GroupMatch(d :: _)) :: _ =>
        val g = d.charAt(0) match {
          case '分' => Minute
          case '天' => Day
          case '年' => Year
        }
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
      case t1 :: _ :: Token(TimeGrain, GrainData(g, _, _)) :: _ =>
        for {
          i <- getIntValue(t1)
          d <- timesOneAndAHalf(g, i.toInt)
        } yield Token(Duration, d)
    }
  )

  val ruleCompositeDuration = Rule(
    name = "composite <duration> <duration>",
    pattern =
      List(isDimension(Duration).predicate, isDimension(Duration).predicate),
    prod = tokens {
      case Token(_, b@DurationData(v, g, _, _, _)) :: Token(_, a@DurationData(_, dg, _, _, _)) :: _ if g > dg =>
        // ❌ 两年三月
        // ✅ 一分三十秒
        if (!b.latent && g != Month && (!a.latent || dg != Hour) || (b.latent && g == Minute)) Token(Duration, b + a)
        else None
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
      case t1 :: Token(TimeGrain, GrainData(g, _, _)) :: _ :: Token(_, dd@DurationData(_, dg, _, _, _)) :: _
        if g > dg && g != Month =>
        for (i <- getIntValue(t1)) yield Token(Duration, DurationData(i.toInt, g, schema = durationSchema(i.toInt.toString, g)) + dd)
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
      case t1 :: Token(TimeGrain, GrainData(g0, _, raw)) :: Token(DigitSequence, DigitSequenceData(s, _, _)) :: Token(TimeGrain, GrainData(g1, _, _)) :: _
        if g0 > g1 && raw != "点" =>
        for (i <- getIntValue(t1)) yield Token(Duration, DurationData(i.toInt, g0, schema = durationSchema(i.toString, g0)) + DurationData(s.toInt, g1, schema = durationSchema(s.toInt.toString, g1)))
    }
  )
}
