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

package com.xiaomi.duckling.dimension.time

import scalaz.Scalaz._

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.ordinal.{Ordinal, OrdinalData}
import com.xiaomi.duckling.dimension.time.Helpers._
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.duration.{Duration, DurationData}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.Hint._
import com.xiaomi.duckling.dimension.time.enums.IntervalType.{Closed, Open}
import com.xiaomi.duckling.dimension.time.enums.{Grain, Hint, IntervalDirection}
import com.xiaomi.duckling.dimension.time.form.{PartOfDay, TimeOfDay}
import com.xiaomi.duckling.dimension.time.grain.{GrainData, TimeGrain}
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.rule._

trait Rules extends DimRules {
  lazy private val enableTimeSeqence = conf.getBoolean("dimension.time.sequence.enable")

  val ruleRecentTime = Rule(
    name = "this/last/next <time>",
    pattern = List(
      "((这|今|本(?!现在))一?个?|明|上+一?个?|前一个?|(?<![一|查|看|搜|记|问|写])下+一?个?)".regex,
      // ❌ 下周一早上 =>  下[周一早上]
      and(isDimension(Time), isNotLatent, not(isHint(Intersect)), not(isAPartOfDay)).predicate
    ),
    prod = tokens {
      case Token(RegexMatch, GroupMatch(s :: _)) :: (t @ Token(Time, td: TimeData)) :: _ =>
        val offset =
          s(0) match {
            case '今' | '这' | '本' => 0
            case '去' | '前'       => -1
            case '明'             => 1
            case '上'             => -1 * s.takeWhile(_ == '上').length
            case '下'             => 1 * s.takeWhile(_ == '下').length
          }

        val isValidCombination = s(0) match { // 病态表达验证
          case '本' => // 本，之和周月组合
            td.timeGrain match {
              case Week | Month => true
              case _ => false
            }
          case '上' | '下' => // 上/下， 不与‘天’和‘确切年’组合，eg:本今天，下二零一九年
            if (td.timeGrain == Day && td.holiday.isEmpty || td.timeGrain == Year) false
            else true
          case _ => true
        }

        if (isValidCombination) {
          val resetGrain = if (isADayOfWeek.isDefinedAt(t) && isADayOfWeek(t)) Week else td.timeGrain
          (predNth(offset, false) >>> reset(resetGrain) >>> tt)(td)
        } else None
    }
  )

  /**
    * 五月的第三个星期天
    */
  val ruleNthTimeOfTime = Rule(
    name = "nth <time> of <time>",
    pattern =
      List(isDimension(Time).predicate, isDimension(Ordinal).predicate, isNotLatent.predicate),
    prod = tokens {
      case Token(Time, td1: TimeData) ::
            Token(Ordinal, od: OrdinalData)
            :: Token(Time, td2: TimeData) :: _ if td1.timeGrain > td2.timeGrain && od.ge =>
        val join: Option[TimeData] = intersect(td2, td1)
        join match {
          case Some(x) =>
            tt(predNthAfter(od.value.toInt - 1, td2, td1))
          case None => None
        }
    }
  )

  val ruleNthTimeOfTime2 = Rule(
    name = "nth <time> of <time>2",
    pattern = List(
      isDimension(Time).predicate,
      "的".regex,
      isDimension(Ordinal).predicate,
      isNotLatent.predicate
    ),
    prod = tokens {
      case Token(Time, td1: TimeData) :: _ ::
            Token(Ordinal, od: OrdinalData)
            :: Token(Time, td2: TimeData) :: _ if td1.timeGrain > td2.timeGrain && od.ge =>
        val join: Option[TimeData] = intersect(td2, td1)
        join match {
          case Some(x) =>
            (predNth(od.value.toInt - 1, false) >>> tt)(x)
          case None => None
        }
    }
  )

  val ruleIntersect =
    Rule(
      name = "intersect",
      // 左右边均不能是中午/下午，避免与<dim time> <part-of-day>冲突
      pattern = List(
        and(isDimension(Time), isNotLatent, not(isAPartOfDay)).predicate,
        // "一日"单独是latent，但是可以参与组合
        and(isDimension(Time), or(and(isNotLatent, not(isAPartOfDay)), isADayOfMonth)).predicate
      ),
      prod = tokens {
        case Token(Time, td1: TimeData) :: Token(Time, td2: TimeData) :: _
            if td1.timeGrain > td2.timeGrain && !(td1.hint == Date && td2.hint == Date) =>
          // 破除(y-m)-d和y-(m-d)均构造出来的问题
          if (td1.hint == YearMonth && td2.hint == DayOnly) None
          // 固定顺序，避免(y-m)-(d H-M-S) 以及(y)-(m-d H-M-S)出现
          else if (td1.timeGrain > Day && td2.timeGrain < Day) None
          // 避免多路解析 [2017年三月2号早上][10点半] 和 [2017年三月2号][早上10点半]
          //          else if (td1.timeGrain == Day && td2.timeGrain == Hour) None
          else {
            val hint =
              if (td1.timeGrain == Year && td2.hint == MonthOnly) YearMonth
              else Intersect
            val td = intersect(td1, td2).map(_.copy(hint = hint))
            tt(td)
          }
      }
    )

  val ruleIntersect2 = Rule(
    name = "intersect: <x> 的 <y>",
    // "一日"单独是latent，但是可以参与组合
    pattern = List(isNotLatent.predicate, "的".regex, or(isNotLatent, isADayOfMonth).predicate),
    prod = tokens {
      case Token(Time, td1: TimeData) :: _ :: Token(Time, td2: TimeData) :: _
          if td1.timeGrain > td2.timeGrain =>
        if (td1.timeGrain > Day && td2.timeGrain < Day) None
        else {
          val hint =
            if (td1.timeGrain == Year && td2.hint == MonthOnly) YearMonth
            else NoHint
          val td = intersect(td1, td2).map(_.copy(hint = hint))
          tt(td)
        }
    }
  )

  /**
    * 现在的组合中有一些错误的，后续可以只留下有效的
    */
  val ruleRecentCycle = Rule(
    name = "this <cycle>",
    pattern =
      List("(这个?|今个?|本|下+一?个?|上+一?个?|去|昨|明|大*前|大*后)".regex, isDimension(TimeGrain).predicate),
    prod = tokens {
      case Token(_, GroupMatch(s :: _)) :: Token(TimeGrain, GrainData(g, _)) :: _ =>
        val td: Option[TimeData] = s(0) match {
          case '这' => cycleNthThis(0, g, Year, Month, Hour, Week)
          case '今' => cycleNthThis(0, g, Year, Month, Day)
          case '本' => cycleNthThis(0, g, Year, Month, Day, Week)
          case '明' => cycleNthThis(1, g, Day, Year)
          case '昨' => cycleNthThis(-1, g, Day)
          case '去' => cycleNthThis(-1, g, Year)
          case '后' => cycleNthThis(2, g, Year, Day)
          case '前' => cycleNth(g, if (s.length == 1) -2 else -1) // 前天\前一天
          case '大' =>
            val count = s.takeWhile(_ == '大').length
            val (base, direction) = if (s.last == '前') (-2, -1) else (2, 1)
            cycleNth(g, base + direction * count)
          case '上' | '下' =>
            val count = s.takeWhile(_ == s(0)).length
            val sign = if (s.head == '上') -1 else +1
            // 上一年
            if (g == Grain.Day || g == Grain.Year && s.last != '一') None
            else cycleNth(g, sign * count)
          case _ => throw new NotImplementedError(s"matched item $s missing handler")
        }
        for (t <- td) yield tt(t.copy(hint = RecentNominal))
    }
  )

  private val RecentPattern = "(上|下|前|后|过去|未来|接下来|之前|往前|向前|今后|之后|往后|向后|最近|近|这)"

  /**
    * 时间区间:
    * 未来三天，包括今天
    * 下一周，不包括本周
    * 未来一周 = 未来七天
    * 定义有待明确
    */
  val ruleLastNextNCycle = Rule(
    name = "recent/last/next <duration>",
    pattern = List(RecentPattern.regex, isDimension(Duration).predicate),
    prod = tokens {
      case Token(_, GroupMatch(s :: _)) :: Token(Duration, DurationData(v, g, _, fuzzy)) :: _ =>
        // 月必须是x个月
        s match {
          case "下" | "后" | "接下来" | "未来" | "今后" | "之后" | "向后" | "往后" =>
            val td: Option[TimeData] =
              // 未来一周=未来七天
              if (s == "未来" && v == 1 && g == Week) {
                cycleN(notImmediate = false, Day, 7)
              }
              // 未来一个月等于未来30天
              else if (s == "未来" && v == 1 && g == Month) {
                cycleN(notImmediate = false, Day, 30)
              }
              // = 1 已经在 this <cycle> 中定义过了
              else if (s == "下" && g == Day || v == 1) None
              else {
                val td1 = cycleN(notImmediate = false, g, v)
                g match {
                  case Week => reset(Week)(td1)
                  case _    => td1
                }
              }
            td.map(t => tt(t.at(Hint.Recent)))
          case "这" | "最近" | "近" =>
            if (v > 1) {
              val td: TimeData = cycleN(notImmediate = false, g, v).at(Hint.UncertainRecent)
              g match {
                case Week => (reset(Week) _ >>> tt)(td)
                case _    => tt(td)
              }
            } else tt(cycleNth(g, 0))
          case "上" | "前" | "之前" | "往前"  | "向前" | "过去" | "过去" =>
            if (s == "上" && g == Day) None
            else if (s == "过去" && fuzzy) None
            else if (v > 1) tt(cycleN(notImmediate = true, g, -v).at(Hint.Recent))
            else tt(cycleNth(g, -1).at(Hint.Recent))
          case _ => throw new NotImplementedError(s"matched item $s missing handler")
        }
    }
  )

  /**
    * 时间点 + 时间粒度粒度 1
    */
  val ruleNCycleNextLast1 = Rule(
    name = "n <cycle> next/last 1: <duration> 之后",
    pattern = List(isDimension(Duration).predicate, "((之|以)?(后|前))|过后".regex),
    prod = tokens {
      case Token(Duration, DurationData(v, grain, _, _)) :: Token(_, GroupMatch(s :: _)) :: _ =>
        val offset = if (s.endsWith("后")) v else -v
        tt(cycleNth(grain, offset, NoGrain))
    }
  )

  /**
    * 时间点 + 时间粒度粒度 2
    */
  val ruleNCycleNext2 = Rule(
    name = "n <cycle> next/last: 过 <duration>",
    pattern = List("过".regex, isDimension(Duration).predicate),
    prod = tokens {
      case _ :: Token(Duration, DurationData(v, grain, _, _)) :: _ =>
        tt(cycleNth(grain, v, NoGrain))
    }
  )

  /**
    * 时间点 + 时间粒度粒度 2
    */
  val ruleNCycleNext3 = Rule(
    name = "n <cycle> next/last 3:过 <duration> 之后",
    pattern = List("过".regex, isDimension(Duration).predicate, "之?(后|前)".regex),
    prod = tokens {
      case _ :: Token(Duration, DurationData(v, grain, _, _)) :: _ =>
        tt(cycleNth(grain, v, NoGrain))
    }
  )

  /**
    * 一个月后的下午19点
    */
  val ruleDurationIntersectTime = Rule(
    name = "<duration> before/after <time>",
    pattern = List(isDimension(Duration).predicate, "之?(前|后)的?".regex, isNotLatent.predicate),
    prod = tokens {
      case Token(Duration, DurationData(v, g, _, _)) :: Token(_, GroupMatch(_ :: s :: _)) ::
        Token(Time, td: TimeData) :: _ =>
        if (g > td.timeGrain) {
          val sign = if (s == "前") -1 else 1
          val roundGrain =
            if (g >= Day && td.form.nonEmpty) {
              td.form.get match {
                case PartOfDay(_)    => Day
                case TimeOfDay(_, _) => Day
                case _               => NoGrain
              }
            } else if (g == Year && td.timeGrain == Month) Year
            else NoGrain
          val coarseDate = cycleNth(g, sign * v, roundGrain)
          tt(intersect(coarseDate, td))
        } else None
    }
  )

  val ruleTimeBeforeOfAfter = Rule(
    name = "<time> before/after",
    pattern = List(isDimension(Time).predicate, "[之以]?([前后])".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: Token(_, GroupMatch(_ :: direction :: _)) :: _ =>
        val intervalDirection =
          if (direction == "前") IntervalDirection.Before
          else IntervalDirection.Before
        val predicate = SequencePredicate(
          List(td, TimeData(TimeOpenIntervalPredicate(intervalDirection), timeGrain = Grain.NoGrain))
        )
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )

  /**
    * 时间区间
    */
  val ruleFromToInterval = Rule(
    name = "<from> 到 <to>",
    pattern = List(isNotLatent.predicate, "(至|到)".regex, isNotLatent.predicate),
    prod = tokens {
      case Token(Time, td1 @ TimeData(pred1, _, g1, _, _, _, _, _, _, _, _)) :: _ ::
            Token(Time, td2 @ TimeData(pred2, _, g2, _, _, _, _, _, _, _, _)) :: _ =>
        val grainCompare = for {
          pg1 <- maxPredicateGrain(pred1)
          pg2 <- maxPredicateGrain(pred2)
        } yield pg1 >= pg2
        // 限定求交的Grain，避免日交月
        val isValid =
          grainCompare.nonEmpty && grainCompare.get && (g1 < Day && g2 < Day || g1 >= Day && g2 >= Day) ||
            g1 >= g2 ||
            td1.hint == RecentNominal || td2.hint == RecentNominal
        if (isValid) {
          val intervalType = if (g1 == g2) {
            g1 match {
              case Year | Month | Day => Closed
              case _                  => Open
            }
          } else Open
          tt(interval(intervalType, td1, td2).map(_.copy(hint = FinalRule)))
        } else None
    }
  )

  /**
    * 时间区间
    */
  val ruleInAInterval = Rule(
    name = "in a <duration>",
    pattern = List(isDimension(Duration).predicate, "内".regex),
    prod = tokens {
      case Token(Duration, DurationData(value, grain, _, _)) :: _ =>
        tt(cycleN(notImmediate = false, grain, value, NoGrain))
    }
  )

  def recentHint(hint: Hint): Boolean = {
    hint match {
      case Recent        => true
      case RecentNominal => true
      case _             => false
    }
  }

  def sequenceProd(td1: TimeData, td2: TimeData): Option[Token] = {
    if (!enableTimeSeqence) None
    else {
      (td1.timePred, td2.timePred) match {
        case (_: TimeDatePredicate, _: TimeDatePredicate) => None
        case _ =>
          if (td1.timeGrain >= td2.timeGrain) {
            val cal = calendar(td1, td2)
            if (td1.timeGrain > td2.timeGrain && recentHint(td2.hint)) {
              tt(
                TimeData(
                  ReplacePartPredicate(td1, td2),
                  timeGrain = td2.timeGrain,
                  hint = ReplacePartOfTime,
                  calendar = cal
                )
              )
            }
            // 保守约束 [明天]的[之后三天]
            else if (recentHint(td2.hint) || recentHint(td1.hint) && td1.timeGrain == td2.timeGrain) {
              val pred = SequencePredicate(List(td1, td2))
              tt(TimeData(pred, timeGrain = td2.timeGrain, hint = Hint.Sequence, calendar = cal))
            } else None
          } else None
      }
    }
  }

  /**
    * 明天的后天/1988年的今天
    */
  val ruleSequence = Rule(
    name = "sequence1: tomorrow of tomorrow",
    pattern = List(
      isDimension(Time).predicate,
      "(之后)?的".regex,
      and(isNotHint(ReplacePartOfTime), isNotHint(Sequence)).predicate
    ),
    prod = tokens {
      case Token(_, td1: TimeData) :: _ :: Token(_, td2: TimeData) :: _ =>
        sequenceProd(td1, td2)
    }
  )

  /**
    * 明天的后天/1988年的今天
    */
  val ruleSequence2 = Rule(
    name = "sequence2: this day of last month",
    pattern = List(
      and(isNotLatent, isGrainGeDay).predicate,
      and(isHint(RecentNominal, Recent), isNotHint(Sequence)).predicate
    ),
    prod = tokens {
      case Token(_, td1: TimeData) :: Token(_, td2: TimeData) :: _
          if td1.timeGrain > td2.timeGrain ||
            td1.timeGrain == td2.timeGrain && !(recentHint(td1.hint) && recentHint(td2.hint)) =>
        sequenceProd(td1, td2)
    }
  )

  /**
    * 明天之后的三天
    */
  val ruleSequence3 = Rule(
    name = "sequence3: <recent nominal> <duration>",
    pattern =
      List(isHint(RecentNominal).predicate, "(往|向|之)?(前|后)的?".regex, isDimension(Duration).predicate),
    prod = tokens {
      case Token(_, td1: TimeData) :: Token(_, GroupMatch(_ :: s :: _)) :: Token(
            _,
            DurationData(value, grain, latent, _)
          ) :: _ if td1.timeGrain == grain =>
        val sign = if (s == "前") -1 else 1
        val td2 = cycleN(notImmediate = false, grain, sign * value)
        val pred = SequencePredicate(List(td1, td2))
        tt(TimeData(pred, timeGrain = td2.timeGrain))
    }
  )

  val ruleEndOfGrain = Rule(
    name = "end of grain",
    pattern = List(and(isTimeDatePredicate, or(isAMonth, isAYear)).predicate, "(月|年)?底".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: Token(RegexMatch, GroupMatch(s :: _)) :: _ =>
        if (s.startsWith("年") && td.timeGrain != Year) None
        else if (s.startsWith("月") && td.timeGrain != Month) None
        else {
          val grain = td.timeGrain match {
            case Year  => Day
            case Month => Day
            case _     => throw new IllegalStateException("may not happen")
          }
          val td1 = TimeData(EndOfGrainPredicate, timeGrain = grain)
          tt(TimeData(SequencePredicate(List(td, td1)), timeGrain = grain))
        }
    }
  )

  def openInterval(td: TimeData, direction: IntervalDirection): Option[Token] = {
    val pred = SequencePredicate(List(td, TimeData(TimeOpenIntervalPredicate(direction), timeGrain = Grain.NoGrain)))
    tt(TimeData(pred, timeGrain = td.timeGrain, hint = Hint.Sequence))
  }

  val composite = List(
    ruleRecentTime,
    ruleNthTimeOfTime,
    ruleNthTimeOfTime2,
    ruleIntersect,
    ruleIntersect2,
    ruleRecentCycle,
    ruleLastNextNCycle,
    ruleNCycleNextLast1,
    ruleNCycleNext2,
    ruleNCycleNext3,
    ruleFromToInterval,
    ruleDurationIntersectTime,
    ruleInAInterval,
    ruleSequence,
    ruleSequence2,
    ruleSequence3,
    ruleEndOfGrain,
    ruleTimeBeforeOfAfter
  )

  override def dimRules: List[Rule] =
    composite ++
      Dates.rules ++
      Weeks.rules ++
      FuzzyDayIntervals.rules ++
      Times.rules ++
      Holidays.rules ++
      SolarTerms.rules
}
