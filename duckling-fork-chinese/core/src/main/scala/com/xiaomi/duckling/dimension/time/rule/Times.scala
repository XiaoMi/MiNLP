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

package com.xiaomi.duckling.dimension.time.rule

import org.apache.commons.lang3.StringUtils

import scalaz.Scalaz._

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.regexMatch
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.numeral.NumeralData
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.numeral.Prods.integerMap
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.enums.Hint
import com.xiaomi.duckling.dimension.time.form.TimeOfDay
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.{Time, TimeData}

object Times {

  val ruleNow = Rule(name = "now", pattern = List("(现在|此时(此刻)?|此刻|当前|这会儿?)".regex), prod = tokens {
    case _ => tt(now)
  })

  val ruleHhmmssTimeOfDay = Rule(
    name = "hh:mm[:ss] (time-of-day)",
    pattern = List(raw"((?:[01]?\d)|(?:2[0-3])):([0-5]\d)(:([0-5]\d))?".regex),
    prod = regexMatch {
      case _ :: hh :: mm :: _ :: ss :: _ =>
        for {
          h <- parseInt(hh).toOption
          m <- parseInt(mm).toOption
        } yield {
          val s = parseInt(ss).toOption
          tt(hourMinuteSecond(is12H = 0 < h && h <= 12, h, m, s))
        }
    }
  )

  val ruleHhmmssCN_TimeOfDay = Rule(
    name = "hh时mm分ss秒 (time-of-day)",
    pattern = List(raw"((?:[01]?\d)|(?:2[0-3]))[点]([0-5]?\d)分(([0-5]?\d)秒)?".regex),
    prod = regexMatch {
      case _ :: hh :: mm :: _ :: ss :: _ =>
        for {
          h <- parseInt(hh).toOption
          m <- parseInt(mm).toOption
        } yield {
          val s = parseInt(ss).toOption
          tt(hourMinuteSecond(is12H = 0 < h && h <= 12, h, m, s))
        }
    }
  )

  val ruleTimeOfDayOClock = Rule(
    name = "<time-of-day> o'clock",
    pattern = List(and(isIntegerBetween(0, 24), not(isCnSequence)).predicate, "(点(整|钟)?|时)".regex),
    prod = tokens {
      case (t1@Token(_, nd: NumeralData)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        if (nd.isCnSeq) None
        else
          for (h <- getIntValue(t1)) yield {
            val latent = if (h == 0 && s == "点") true else false
            val td = hour(is12H = 0 < h && h <= 12 , h.toInt).copy(latent = latent, hint = Hint.NoHint)
            tt(td)
          }
    }
  )

  val ruleIntegerLatentTimeOfDay = Rule(
    name = "<integer> (latent time-of-day)",
    pattern = List(isIntegerBetween(0, 23).predicate),
    prod = tokens {
      case token :: _ =>
        for {
          v <- getIntValue(token)
          t <- tt(hour(is12H = v <= 12, v.toInt).copy(latent = true))
        } yield t
    }
  )

  val ruleDimTimePartOfDay = Rule(
    name = "<dim time> <part-of-day>",
    pattern = List(isADayOfMonth.predicate, and(isAPartOfDay, isNotLatent).predicate),
    prod = tokens {
      case Token(Time, td1: TimeData) :: Token(Time, td2: TimeData) :: _ =>
        for (td <- intersect(td1, td2)) yield {
          tt(td.copy(form = td2.form, hint = Hint.PartOfDayAtLast))
        }
    }
  )

  val rulePartOfDayDimTime = Rule(
    name = "<part-of-day> <dim time>",
    pattern = List(isAPartOfDay.predicate, isNotLatent.predicate),
    prod = tokens {
      case Token(Time, td1: TimeData) :: Token(Time, td2: TimeData) :: _ =>
        intersect(td1, td2).flatMap(tt)
    }
  )

  val ruleRelativeMinutesAfterPastIntegerOClockOfDay = Rule(
    name = "<integer> o'clock (hour-of-day): 八点",
    pattern = List(isAnHourOfDay.predicate, "点钟?".regex),
    prod = tokens {
      case Token(Time, TimeData(_, _, _, _, Some(TimeOfDay(Some(h), _)), _, _, _, _, _, _, _)) :: _ :: _ =>
        for {
          t <- tt(hour(is12H = true, h))
        } yield t
    }
  )

  val ruleRelativeMinutesAfterPastIntegerHourOfDay = Rule(
    name = "<HH:mm>: 八点五十",
    pattern = List(isAnHourOfDay.predicate, "点".regex, isIntegerBetween(10, 59).predicate),
    prod = tokens {
      case Token(Time, TimeData(_, _, _, _, Some(TimeOfDay(Some(hour), _)), _, _, _, _, _, _, _)) :: _ :: token :: _ =>
        for {
          n <- getIntValue(token)
          t <- tt(hourMinuteSecond(is12H = true, hour, n.toInt))
        } yield t
    }
  )

  val ruleRelativeMinutesAfterPastIntegerHourAndHalfOfDay = Rule(
    name = "half (hour-of-day):一点半/一十/二十五",
    pattern = List(isAnHourOfDay.predicate, "点(半|零[一二三四五六七八九]|0[123456789]|一十)分?".regex),
    prod = tokens {
      case Token(Time, TimeData(_, _, _, _, Some(TimeOfDay(Some(hour), _)), _, _, _, _, _, _, _)) ::
            Token(RegexMatch, GroupMatch(_ :: g1 :: _)) :: _ =>
        val m =
          if (g1 == "半") 30
          else if (g1(0) == '零') integerMap(g1.substring(1))
          else if (g1(0) == '0') g1.substring(1).toInt
          else 10
        tt(hourMinuteSecond(is12H = true, hour, m))
    }
  )

  val ruleOfDayHourMinute = Rule(
    name = "<time-of-day>:16点30分/八点一刻",
    pattern = List(
      isAnHourOfDay.predicate,
      "(点过?|时)".regex,
      isIntegerBetween(0, 59).predicate,
      "(分(?!贝)钟?|刻)".regex
    ),
    prod = tokens {
      case Token(Time, TimeData(_, _, _, _, Some(TimeOfDay(Some(h), is12H)), _, _, _, _, _, _, _)) :: _ :: t1
            :: Token(RegexMatch, GroupMatch(u :: _)) :: _ =>
        val mOpt = getIntValue(t1).map(_.toInt)
        if (mOpt.isEmpty) None
        else {
          val m = mOpt.get.toInt
          if (u.startsWith("分")) tt(hourMinuteSecond(is12H = is12H, h, m))
          else if (u == "刻" && m >= 1 && m <= 3) tt(hourMinuteSecond(is12H = is12H, h, 15 * m))
          else None
        }
    }
  )

  val ruleTimeOfDayAMPM = Rule(
    name = "<time-of-day> AM|PM",
    pattern = List(isATimeOfDay.predicate, "(?i)([ap])(\\s|\\.)?m\\.?".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: Token(RegexMatch, GroupMatch(_ :: ap :: _)) :: _ =>
        tt(timeOfDayAMPM(ap == "a", td))
    }
  )

  val rules = List(
    ruleNow,
    ruleHhmmssTimeOfDay,
    ruleTimeOfDayOClock,
    ruleIntegerLatentTimeOfDay,
    ruleDimTimePartOfDay,
    ruleHhmmssCN_TimeOfDay,
    // ruleRelativeMinutesAfterPastIntegerOClockOfDay,
    ruleRelativeMinutesAfterPastIntegerHourOfDay,
    ruleRelativeMinutesAfterPastIntegerHourAndHalfOfDay,
    ruleTimeOfDayAMPM,
    ruleOfDayHourMinute
  )

}
