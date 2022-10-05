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

package com.xiaomi.duckling.dimension.time.date

import scalaz.std.string.parseInt

import java.time.LocalDate
import scala.util.Try

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.GroupMatch
import com.xiaomi.duckling.dimension.matcher.Prods.{regexMatch, singleRegexMatch}
import com.xiaomi.duckling.dimension.numeral.Predicates.{getIntValue, isIntegerBetween}
import com.xiaomi.duckling.dimension.numeral.seq.{DigitSequence, DigitSequenceData}
import com.xiaomi.duckling.dimension.time.Prods.intersectDOM
import com.xiaomi.duckling.dimension.time.duration.{Duration, DurationData, isADecade}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.Hint.{NoHint, RecentNominal, YearMonth}
import com.xiaomi.duckling.dimension.time.enums.IntervalType.{Closed, Open}
import com.xiaomi.duckling.dimension.time.enums.{Grain, Hint}
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.{EndOfGrainPredicate, SequencePredicate, TimeDatePredicate, _}
import com.xiaomi.duckling.dimension.time.{GrainWrapper, TimeData}

trait Rules extends DimRules {

  def adjustYear(y: Int, yy: String): Option[Int] = {
    if (yy.length == 2) {
      if (y < 30) 2000 + y
      else 1900 + y
    } else if (yy.length == 4) y
    else None
  }

  private def ymdRule(name: String, pattern: ItemRegex): Rule = {
    Rule(
      name = name,
      pattern = List(pattern),
      prod = regexMatch {
        case _ :: _ :: yy :: mm :: dd :: _ =>
          for {
            y0 <- parseInt(yy).toOption
            y <- adjustYear(y0, yy)
            m <- parseInt(mm).toOption
            d <- parseInt(dd).toOption
          } yield {
            val tp = TimeDatePredicate(year = y, month = m, dayOfMonth = d)
            Token(Date, TimeData(tp, timeGrain = Day))
          }
      }
    )
  }

  def ymdPattern(split: Char): ItemRegex = {
    val c = if (split == '.') "\\." else split

    val head = s"(^|(?=[^$c\\d]))"
    val tail = s"($$|(?=[^$c\\d]))"
    val s = s"$head(\\d{2,4})$c(0?[1-9]|1[0-2])$c(3[01]|[12]\\d|0?[1-9])$tail"
    s.regex
  }

  val ruleYmd1 = ymdRule("yyyy-MM-dd", ymdPattern('-'))
  val ruleYmd2 = ymdRule("yyyy.MM.dd", ymdPattern('.'))
  val ruleYmd3 = ymdRule("yyyy,MM,dd", ymdPattern(','))
  val ruleYmd4 = ymdRule("yyyy/MM/dd", ymdPattern('/'))

  val ruleYyyyMMddNoDash = Rule(
    name = "date-yyyyMMdd",
    pattern = List(raw"(20\d{2})(\d{2})(\d{2})".regex),
    prod = regexMatch {
      case _ :: yy :: mm :: dd :: _ =>
        for {
          y <- parseInt(yy).toOption
          m <- parseInt(mm).toOption
          d <- parseInt(dd).toOption
          date <- Try {
            LocalDate.of(y, m, d)
          }.toOption
        } yield {
          Token(
            Date,
            TimeData(TimeDatePredicate(year = y, month = m, dayOfMonth = d), timeGrain = Day)
          )
        }
    }
  )

  def mdPattern(split: Char): ItemRegex = {
    val c = if (split == '.') "\\." else split
    val head = s"(^|(?=[^$c\\d]))"
    val tail = s"($$|(?=[^$c\\d]))"
    val s = s"$head(\\d{1,2})$c(\\d{1,2})$tail"
    s.regex
  }

  private def mmddRule(name: String, split: Char): Rule = {
    Rule(
      name = name,
      pattern = List(mdPattern(split)),
      prod = regexMatch {
        case _ :: _ :: mm :: dd :: _ =>
          for {
            m <- parseInt(mm).toOption if m > 0 && m <= 12
            d <- parseInt(dd).toOption if d > 0 && d <= 31
          } yield {
            Token(Date, TimeData(TimeDatePredicate(month = m, dayOfMonth = d), timeGrain = Day))
          }
      }
    )
  }

  val ruleMMdd1 = mmddRule("MM-dd", '-')
  val ruleMMdd2 = mmddRule("MM.dd", '.')
  val ruleMMdd3 = mmddRule("MM/dd", '/')

  val rule_yyyyMM1 = Rule(
    name = "date - yyyy/mm",
    pattern = List("(\\d{4})[-./](\\d{1,2})".regex),
    prod = regexMatch {
      case _ :: yyyy :: mm :: _ =>
        for {
          y <- parseInt(yyyy).toOption if y > 1950 && y <= 2050
          m <- parseInt(mm).toOption if m > 0 && m <= 12
        } yield {
          Token(Date, TimeData(TimeDatePredicate(year = y, month = m), timeGrain = Month))
        }
    }
  )

  val ruleYearNumericWithYearSymbol = Rule(
    name = "date - year (numeric with year symbol)",
    pattern = List(seqYearOf1000to9999.predicate, "(年?版|年)".regex),
    prod = tokens {
      case token :: _ => getIntValue(token).map(i => Token(Date, year(i.toInt)))
    }
  )

  val ruleYearNumericWithoutYearSymbol = Rule(
    name = "date - year (numeric without year symbol)",
    pattern = List(arabicSeqOf1950to2050.predicate),
    prod = tokens {
      case Token(DigitSequence, DigitSequenceData(seq, zh, raw)) :: _ =>
        val y = seq.toDouble
        (if (y >= 1950 && y <= 2050) year(y.toInt)
        else latentYear(y.toInt)).map(td => Token(Date, td))
    }
  )

  val ruleYearCnSequenceWithoutYearSymbol = Rule(
    name = "date - year (cn without year symbol)",
    pattern = List(cnSeqOf1950to2050.predicate),
    prod = tokens {
      case Token(DigitSequence, DigitSequenceData(seq, _, _)) :: _ =>
        Token(Date, year(seq.toInt))
    }
  )

  val singleNumberPredicate = singleNumeber.predicate

  val ruleTwoDigitYear = Rule(
    name = "date - year (like 九八年)",
    pattern = List(singleNumberPredicate, singleNumberPredicate, "(年?版|年)".regex),
    prod = tokens {
      case t1 :: t2 :: _ =>
        val y = for {
          thirdDigit <- getIntValue(t1)
          fourthDigit <- getIntValue(t2)
        } yield {
          val lastTwo = thirdDigit * 10 + fourthDigit
          if (thirdDigit >= 3) 1900 + lastTwo
          else 2000 + lastTwo
        }
        y.map(i => Token(Date, year(i.toInt)))
    }
  )

  val ruleTowDigitYear06 = Rule(
    name = "date - year (like 06年)",
    pattern = List(raw"\d{2}".regex, "(年?版|年)".regex),
    prod = singleRegexMatch {
      case s =>
        val lastTwo = Integer.parseInt(s)
        val y = adjustYear(lastTwo, s).get
        Token(Date, year(y))
    }
  )

  val ruleMonthNumericWithMonthSymbol = Rule(
    name = "date: month (numeric with month symbol)",
    pattern = List(isIntegerBetween(1, 12).predicate, "月(份)?".regex),
    prod = tokens {
      case token :: _ =>
        for (m <- getIntValue(token)) yield {
          Token(Date, month(m.toInt))
        }
    }
  )

  val ruleDayOfMonthWithSymbol = Rule(
    name = "date: day of month (numeric with day symbol)",
    pattern = List(isIntegerBetween(1, 31).predicate, "(号|日)".regex),
    prod = tokens {
      case token :: Token(_, GroupMatch(s :: _)) :: _ =>
        for (d <- getIntValue(token)) yield {
          // 单独的一日/七日 与3号这样的还是有区别的
          Token(Date, dayOfMonth(d.toInt).copy(latent = s == "日"))
        }
    }
  )

  val ruleNamedMonthDayOfMonth = Rule(
    name = "date: <named-month> <day-of-month>",
    pattern =
      List(and(isAMonth, isHint(Hint.MonthOnly)).predicate, isIntegerBetween(1, 31).predicate),
    prod = tokens {
      case Token(Date, td: TimeData) :: token :: _ =>
        for (td <- intersectDOM(td, token)) yield {
          Token(Date, td.at(Hint.MonthDay))
        }
    }
  )

  val ruleRecentNominalDays = Rule(
    name = "date: recent days: 今/明/后..天",
    pattern = List("(今|明|昨)儿".regex),
    prod = regexMatch {
      case _ :: day :: _ =>
        val offset =
          day match {
            case "今" => 0
            case "明" => 1
            //            case "后" => 2
            case "昨" => -1
            //            case "前" => -2
          }
        Token(Date, cycleNth(Day, offset).copy(hint = RecentNominal))
    }
  )

  val ruleDecade = Rule(
    name = "date: tens of decade",
    pattern = List(isADecade.predicate, "年代".regex),
    prod = tokens {
      case t1 :: _ =>
        val y = getIntValue(t1).get.toInt
        val adjustY = if (y > 20) 1900 + y else y + 2000
        val from = year(adjustY)
        val to = year(adjustY + 10)
        for (td <- interval(Open, from, to)) yield Token(Date, td)
    }
  )

  val ruleQuarter = Rule(
    name = "date - <ordinal> <quarter>",
    pattern = List("第".regex, isAQuarterOfYear.predicate),
    prod = tokens {
      case _ :: Token(Duration, DurationData(value, _, _, _, _)) :: _ =>
        for (td <- interval(Closed, month(3 * value - 2), month(3 * value))) yield {
          Token(Date, td.copy(reset = (Grain.resetTo(Quarter), 0)))
        }
    }
  )

  val ruleSpecial0 = Rule(name = "date - special days: 今明两天", pattern = List("今明两天".regex), prod = tokens {
    case _ =>
      val from = cycleNth(Day, 0, Day)
      val to = cycleNth(Day, 1, Day)
      for (td <- interval(Open, from, to)) yield Token(Date, td)
  })

  val ruleSpecial1 = Rule(name = "date - special days: 元月", pattern = List("元月份?".regex), prod = tokens {
    case _ => Token(Date, month(1))
  })

  val ruleSpecial2 = Rule(
    name = "date - special days: 最近",
    pattern = List("(最近|近期|这段时间)".regex),
    prod = tokens {
      case _ =>
        val from = cycleNth(Day, 0, Day)
        val to = cycleNth(Day, 2, Day)
        for (td <- interval(Open, from, to)) yield {
          Token(Date, td.copy(hint = Hint.UncertainRecent))
        }
    }
  )

  val ruleSpecial3 = Rule(name = "date - special days: 明后天", pattern = List("明后两?天".regex), prod = tokens {
    case _ =>
      val from = cycleNth(Day, 1, Day)
      val to = cycleNth(Day, 2, Day)
      for (td <- interval(Open, from, to)) yield Token(Date, td)
  })

  val ruleEndOfMonth = Rule(name = "date - 月底", pattern = List("月(底|末)".regex), prod = tokens {
    case _ =>
      val td1 = TimeData(EndOfGrainPredicate, timeGrain = Day)
      val td = TimeData(SequencePredicate(List(cycleNth(Month, 0), td1)), timeGrain = Day)
      Token(Date, td)
  })

  val ruleHalfYear = Rule(name = "date - 上/下半年", pattern = List("(上|下)半年".regex), prod = regexMatch {
    case _ :: half :: _ =>
      val start = if (half == "上") 0 else 6
      val from = cycleNth(Month, start, Year)
      val to = cycleNth(Month, start + 6, Year)
      for (td <- interval(Open, from, to)) yield {
        Token(Date, td)
      }
  })

  val ruleIntersect =
    Rule(
      name = "dates: intersect",
      pattern = List(
        and(isDimension(Date), isNotLatent).predicate,
        // "一日"单独是latent，但是可以参与组合
        and(isDimension(Date), or(isNotLatent, isADayOfMonth)).predicate
      ),
      prod = tokens {
        case Token(Date, td1: TimeData) :: Token(Date, td2: TimeData) :: _
            if td1.timeGrain > td2.timeGrain =>
          // 破除(y-m)-d和y-(m-d)均构造出来的问题
          if (td1.hint == YearMonth && td2.hint == Hint.DayOnly) None
          // 固定顺序，避免(y-m)-(d H-M-S) 以及(y)-(m-d H-M-S)出现
          else if (td1.timeGrain > Day && td2.timeGrain < Day) None
          // 避免多路解析 [2017年三月2号早上][10点半] 和 [2017年三月2号][早上10点半]
          //          else if (td1.timeGrain == Day && td2.timeGrain == Hour) None
          else {
            // 十月不能与十月一日求交
            val isAlreadySet = td2.timePred match {
              case tdp: TimeDatePredicate =>
                td1.timeGrain match {
                  case Year => tdp.year.nonEmpty
                  case Month => tdp.month.nonEmpty
                  case Day => tdp.dayOfMonth.nonEmpty
                  case _ => true
                }
              case _ => false
            }
            if (isAlreadySet) None
            else {
              val hint =
                if (td1.timeGrain == Year && td2.hint == Hint.MonthOnly) YearMonth
                else NoHint
              for (td <- intersect(td1, td2)) yield {
                Token(Date, td.copy(hint = hint))
              }
            }
          }
      }
    )

  val ruleIntersect2 = Rule(
    name = "dates: intersect: <x> 的 <y>",
    // "一日"单独是latent，但是可以参与组合
    pattern = List(
      and(isDimension(Date), isNotLatent).predicate,
      "的".regex,
      and(isDimension(Date), or(isNotLatent, isADayOfMonth)).predicate
    ),
    prod = tokens {
      case Token(Date, td1: TimeData) :: _ :: Token(Date, td2: TimeData) :: _
          if td1.timeGrain > td2.timeGrain =>
        if (td1.timeGrain > Day && td2.timeGrain < Day) None
        else {
          val hint =
            if (td1.timeGrain == Year && td2.hint == Hint.MonthOnly) YearMonth
            else NoHint
          for (td <- intersect(td1, td2)) yield {
            Token(Date, td.copy(hint = hint))
          }
        }
    }
  )
}
