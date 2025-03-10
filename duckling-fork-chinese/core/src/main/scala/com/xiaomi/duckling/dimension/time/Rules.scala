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

import org.apache.commons.lang3.StringUtils

import scalaz.Scalaz._

import com.xiaomi.duckling.Types.{TokensProduction, _}
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, RegexMatch}
import com.xiaomi.duckling.dimension.numeral.NumeralData
import com.xiaomi.duckling.dimension.numeral.Predicates.{isInteger, isNatural}
import com.xiaomi.duckling.dimension.ordinal.{Ordinal, OrdinalData}
import com.xiaomi.duckling.dimension.time.Helpers._
import com.xiaomi.duckling.dimension.time.Prods._
import com.xiaomi.duckling.dimension.time.duration.{isFuzzyNotLatentDuration, isNotLatentDuration, Duration, DurationData}
import com.xiaomi.duckling.dimension.time.enums.{Grain, Hint, IntervalDirection}
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.enums.Hint._
import com.xiaomi.duckling.dimension.time.enums.IntervalType.{Closed, Open}
import com.xiaomi.duckling.dimension.time.form.{DayOfWeek, PartOfDay, TimeOfDay, Weekend}
import com.xiaomi.duckling.dimension.time.grain.{GrainData, TimeGrain}
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates._
import com.xiaomi.duckling.dimension.time.rule._

trait Rules extends DimRules {
  val ruleRecentTime = Rule(
    name = "this/last/next <time>",
    pattern = List(
      "((这|今|本(?!现在))一?个?|明|上+一?个?|前一个?|(?<![一|查|看|搜|记|问|写])下+一?个?)".regex,
      // ❌ 下周一早上 =>  下[周一早上]
      and(isDimension(Time), isNotLatent, not(isHint(Intersect, FinalRule)), not(isAPartOfDay)).predicate
    ),
    prod = tokens {
      case Token(RegexMatch, GroupMatch(s :: _)) :: (t @ Token(Time, td: TimeData)) :: _ =>
        val offset =
          s(0) match {
            case '今' | '这' | '本' => 0
            case '去' | '前'       => -1
            case '明'             => 1
            case '上'             => -1 * s.takeWhile(_ == '上').length
            case '下'             =>
              val n = 1 * s.takeWhile(_ == '下').length
              if (td.holiday.nonEmpty && n > 0) n - 1 else n
          }

        val isValidCombination = s(0) match { // 病态表达验证
          case '本' => // 本，之和周月组合
            td.timeGrain match {
              case Week | Month => true
              case Day if td.form.contains(Weekend) => true // 本周末
              case _ => false
            }
          case '上' | '下' => // 上/下， 不与‘天’和‘确切年’组合，eg:本今天，下二零一九年
            if (td.timeGrain == Day && td.holiday.isEmpty && !td.form.contains(Weekend) // 下周末是可以的
              || td.timeGrain == Year) false
            else true
          case _ => true
        }

        if (isValidCombination) {
          val resetGrain = if (isADayOfWeek(t) || isWeekend(t)) Week else td.timeGrain
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
  
  /**
    * 今年/本月/2024年/5月 + 最后一天/(倒数)?第二天
    */
  val ruleNthTimeOfOrdinalGrain = Rule(
    name = "nth <time> of <ordinal> grain",
    pattern =
      List(isDimension(Time).predicate, isDimension(Ordinal).predicate, isDimension(TimeGrain).predicate),
    prod = tokens {
      case Token(Time, td: TimeData) ::
        Token(Ordinal, od: OrdinalData)
        :: Token(TimeGrain, GrainData(g, _, _)) :: _ if td.timeGrain > g && g == Grain.Day && !od.ge =>
        
        val predicate = if (od.value >= 0) {
          val ov = if(od.value > 0) od.value.toInt -1 else 0
          SequencePredicate(List(td, cycleNth(g, ov)))
        } else {
          SequencePredicate(List(td, cycleNth(td.timeGrain, 1), cycleNth(g, od.value.toInt)))
        }
        
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )
  
  val ruleNthTimeOfOrdinalGrain2 = Rule(
    name = "nth <time> of <ordinal> grain2",
    pattern = List(
      isDimension(Time).predicate,
      "的".regex,
      isDimension(Ordinal).predicate,
      isDimension(TimeGrain).predicate
    ),
    prod = tokens {
      case Token(Time, td: TimeData) :: _ ::
        Token(Ordinal, od: OrdinalData)
        :: Token(TimeGrain, GrainData(g, _, _)) :: _ if td.timeGrain > g && g == Grain.Day && !od.ge =>
        
        val predicate = if (od.value >= 0) {
          val ov = if(od.value > 0) od.value.toInt -1 else 0
          SequencePredicate(List(td, cycleNth(g, ov)))
        } else {
          SequencePredicate(List(td, cycleNth(td.timeGrain, 1), cycleNth(g, od.value.toInt)))
        }
        
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )

  private def intersectToken(options: Options, td1: TimeData, td2: TimeData): Option[Token] = {
    // 破除(y-m)-d和y-(m-d)均构造出来的问题
    if (td1.hint == YearMonth && td2.hint == DayOnly) None
    // 固定顺序，避免(y-m)-(d H-M-S) 以及(y)-(m-d H-M-S)出现
    else if (td1.timeGrain > Day && td2.timeGrain < Day) None
    // 避免多路解析 [2017年三月2号早上][10点半] 和 [2017年三月2号][早上10点半]
    //          else if (td1.timeGrain == Day && td2.timeGrain == Hour) None
    else {
      val hint =
        if (td1.timePred.isInstanceOf[SequencePredicate]) Sequence
        else if (td1.timeGrain == Year && td2.hint == MonthOnly) YearMonth
        else if (td2.hint == Hint.PartOfDay || td2.form.nonEmpty) Hint.PartOfDay
        else Intersect
      val form = (td1.form, td2.form) match {
        case (Some(PartOfDay(_)), Some(tod: TimeOfDay)) => Some(tod.copy(is12H = false))
        case (Some(DayOfWeek), Some(_: TimeOfDay)) => td2.form
        case (None, _) => td2.form
        case _ => None
      }
      // 10号八点，需要去掉AMPM (今天是10号9点时，不应再出20点)
      // 今天8点，需要根据当前时间是出8/20点
      val _td2 = if (td1.hint == Hint.RecentNominal) td2 else removeAMPM(td2)
      val _td1 = FuzzyDayIntervals.enlarge(td1, options.timeOptions.beforeEndOfInterval)
      hint match {
        case Sequence => sequenceProd(_td1, _td2)
        case _ => tt(intersect(_td1, _td2).map(_.copy(hint = hint, form = form)))
      }
    }
  }

  val ruleIntersect =
    Rule(
      name = "intersect",
      // 左右边均不能是中午/下午，避免与<dim time> <part-of-day>冲突
      pattern = List(
        and(isDimension(Time), isNotLatent, isNotHint(Hint.Season)).predicate,
        // "一日"单独是latent，但是可以参与组合
        and(isDimension(Time), or(and(or(isNotLatent, isLatent0oClockOfDay), not(isAPartOfDay)), isADayOfMonth)).predicate
      ),
      prod = { case (options, (t1@Token(Time, td1: TimeData)) :: (t2@Token(Time, td2: TimeData)) :: _)
          if td1.timeGrain > td2.timeGrain && !(td1.hint == Hint.Date && td2.hint == Hint.Date) ||
            // 上午的8-9点
            td1.timeGrain == td2.timeGrain && td1.timeGrain == Hour && isAPartOfDay(t1) && !isAPartOfDay(t2) =>
        intersectToken(options, td1, td2)
      }
    )

  val ruleIntersect2 = Rule(
    name = "intersect: <x> 的 <y>",
    // "一日"单独是latent，但是可以参与组合
    pattern = List(isNotLatent.predicate, "的".regex, or(or(isNotLatent, isLatent0oClockOfDay), isADayOfMonth).predicate),
    prod = {case (options, (t1@Token(Time, td1: TimeData)) :: _ :: (t2@Token(Time, td2: TimeData)) :: _)
        if td1.timeGrain > td2.timeGrain ||
          // 上午的8-9点
          td1.timeGrain == td2.timeGrain && td1.timeGrain == Hour && isAPartOfDay(t1) && !isAPartOfDay(t2) =>
        intersectToken(options, td1, td2)
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
      case Token(_, GroupMatch(s :: _)) :: Token(TimeGrain, GrainData(g, false, _)) :: _ =>
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

  private val RecentPattern = "(上|下|近|这|前|后面?)|(过去|未来|接下来|之前|往前|向前|今后|之后|往后|向后|最近|将来)的?"

  /**
    * 时间区间:
    * 未来三天，包括今天
    * 下一周，不包括本周
    * 未来一周 = 未来七天
    * 定义有待明确
    */
  val ruleLastNextNCycle = Rule(
    name = "recent/last/next <duration>",
    pattern = List(RecentPattern.regex, or(isNotLatentDuration, isFuzzyNotLatentDuration).predicate),
    prod = tokens {
      case Token(_, GroupMatch(_ :: g1 :: g2 :: _)) :: Token(Duration, DurationData(v, g, _, fuzzy, half, _)) :: _ =>
        val s = if (StringUtils.isBlank(g1)) g2 else g1
        // 月必须是x个月
        s match {
          case "下" | "后" | "接下来" | "未来" | "今后" | "之后" | "向后" | "往后" | "后面" | "将来" =>
            val td: Option[TimeData] =
              // 未来一周=未来七天
              if ((s == "未来" || s == "接下来") && g == Week) {
                cycleN(notImmediate = false, Day, 7 * v)
              }
              // 未来一个月等于未来30天
              else if (s == "未来" && v == 1 && g == Month) {
                cycleN(notImmediate = false, Day, 30)
              }
              // = 1 已经在 this <cycle> 中定义过了
              else if (s == "下" && (g == Day || v == 1 || half)) None
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
            } else if (s != "这" && g == Week) { // 最?近一周，改为最近7天。
              cycleN(notImmediate = false, Day, 7 * v).map(t => tt(t.at(Hint.Recent)))
            } else tt(cycleNth(g, 0))
          case "上" | "前" | "之前" | "往前"  | "向前" | "过去" | "过去" =>
            if (s == "上" && (g == Day || g == Year || half)) None
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
    pattern = List(isNotLatentDuration.predicate, "((之|以)?(后|前))|过后".regex),
    prod = optTokens {
      case (options: Options, Token(Duration, DurationData(v, grain, false, _, _, _)) :: Token(_, GroupMatch(s :: _)) :: _) =>
        val offset = if (s.endsWith("后")) v else -v
        val roundGrain = if (options.timeOptions.inheritGrainOfDuration) grain else NoGrain
        tt(finalRule(cycleNth(grain, offset, roundGrain)))
    }
  )

  /**
    * 时间点 + 时间粒度粒度 2
    */
  val ruleNCycleNext2 = Rule(
    name = "n <cycle> next/last: 过 <duration>",
    pattern = List("过".regex, isNotLatentDuration.predicate),
    prod = optTokens {
      case (options: Options, _ :: Token(Duration, DurationData(v, grain, _, _, _, _)) :: _ ) =>
        val roundGrain = if (options.timeOptions.inheritGrainOfDuration) grain else NoGrain
        tt(finalRule(cycleNth(grain, v, roundGrain)))
    }
  )

  /**
    * 时间点 + 时间粒度粒度 2
    */
  val ruleNCycleNext3 = Rule(
    name = "n <cycle> next/last 3:过 <duration> 之后",
    pattern = List("过".regex, isNotLatentDuration.predicate, "之?(后|前)".regex),
    prod = optTokens {
      case (options: Options, _ :: Token(Duration, DurationData(v, grain, _, _, _, _)) :: _ ) =>
        val roundGrain = if (options.timeOptions.inheritGrainOfDuration) grain else NoGrain
        tt(finalRule(cycleNth(grain, v, roundGrain)))
    }
  )

  /**
    * 一个月后的下午19点
    */
  val ruleDurationIntersectTime = Rule(
    name = "<duration> before/after <time>",
    pattern = List(isNotLatentDuration.predicate, "之?(前|后)的?".regex, isNotLatent.predicate),
    prod = tokens {
      case Token(Duration, DurationData(v, g, _, _, _, _)) :: Token(_, GroupMatch(_ :: s :: _)) ::
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
            } else g
          val coarseDate = cycleNth(g, sign * v, roundGrain)
          tt(intersect(coarseDate, td))
        } else None
    }
  )

  val ruleTimeBeforeOfAfter = Rule(
    name = "<time> before/after",
    pattern = List(and(not(isHint(Hint.UncertainRecent, Hint.Recent)), isNotLatent).predicate, "[之以]?([前后])".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: Token(_, GroupMatch(_ :: direction :: _)) :: _ =>
        val intervalDirection =
          if (direction == "前") IntervalDirection.Before
          else IntervalDirection.After
        val predicate = SequencePredicate(
          List(td, TimeData(TimeOpenIntervalPredicate(intervalDirection), timeGrain = Grain.NoGrain))
        )
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )

  /**
    * 3月5号的八天前/3月5号的两个星期后
    */
  val ruleTimeBeforeOfAfter2 = Rule(
    name = "<time> before/after 2",
    pattern = List(and(isADayOfMonth, isNotLatent).predicate, "的".regex, isNotLatentDuration.predicate, "[之以]?([前后])".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: _ :: Token(Duration, DurationData(v, g, _, _, _, _)) :: Token(_, GroupMatch(_ :: d :: _)) :: _ =>
        val dv = if (d == "前") -v else v
        val dg = if (g <= Day) g else Day
        
        val predicate = SequencePredicate(List(td, cycleNth(g, dv, dg)))
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )
  
  val ruleTimeBeforeOfAfter3 = Rule(
    name = "<time> before/after 3",
    pattern = List(and(isADayOfMonth, isNotLatent).predicate, isNotLatentDuration.predicate, "[之以]?([前后])".regex),
    prod = tokens {
      case Token(Time, td: TimeData) :: Token(Duration, DurationData(v, g, _, _, _, _)) :: Token(_, GroupMatch(_ :: d :: _)) :: _ =>
        val dv = if (d == "前") -v else v
        val dg = if (g <= Day) g else Day
        
        val predicate = SequencePredicate(List(td, cycleNth(g, dv, dg)))
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )
  
  val ruleTimeAfterDuration = Rule(
    name = "<time> after duration",
    pattern = List(isADayOfMonth.predicate, "(再?过|往后(数|推)|后面?的第?)".regex, isNotLatentDuration.predicate),
    prod = tokens {
      case Token(Time, td: TimeData) :: _ :: Token(Duration, DurationData(v, g, _, _, _, _)) :: _ =>
        val dv = v
        val dg = if (g <= Day) g else Day
        
        val predicate = SequencePredicate(List(td, cycleNth(g, dv, dg)))
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )
  
  val ruleTimeBeforeDuration = Rule(
    name = "<time> before duration",
    pattern = List(isADayOfMonth.predicate, "(往前(数|推)|前面?的第?)".regex, isNotLatentDuration.predicate),
    prod = tokens {
      case Token(Time, td: TimeData) :: _ :: Token(Duration, DurationData(v, g, _, _, _, _)) :: _ =>
        val dv = -v
        val dg = if (g <= Day) g else Day
      
        val predicate = SequencePredicate(List(td, cycleNth(g, dv, dg)))
        tt(TimeData(timePred = predicate, timeGrain = td.timeGrain))
    }
  )

  /**
    * 时间区间
    */
  val ruleFromToInterval = Rule(
    name = "<from> 到 <to>",
    pattern = List(isNotLatent.predicate, "(至|到|~)".regex, isNotLatent.predicate),
    prod = {
      case (options, Token(Time, td1 @ TimeData(pred1, _, g1, _, _, _, _, _, _, _, _, _)) :: _ ::
            Token(Time, td2 @ TimeData(pred2, _, g2, _, _, _, _, _, _, _, _, _)) :: _) =>
        // 限定求交的Grain，避免日交月
        val m1 = maxPredicateGrain(pred1)
        val m2 = maxPredicateGrain(pred2)
        val isValid =
           (g1 < Day && g2 < Day || g1 >= Day && (g1 == g2 || m1.nonEmpty && m1 == m2)) ||
            g1 >= g2 ||
            td1.hint == RecentNominal || td2.hint == RecentNominal
        if (isValid) {
          val intervalType = if (g1 == g2) {
            g1 match {
              case Year | Month | Day => Closed
              case _                  => Open
            }
          } else Open
          tt(interval(intervalType, td1, td2, options.timeOptions.beforeEndOfInterval).map(_.copy(hint = FinalRule)))
        } else None
    }
  )

  /**
   * 3到5号，3到5点，3到5月
   */
  val ruleFromToIntervalAbbr1 = Rule(
    name = "<from>(no grain) 到 <to>",
    pattern = List(isNatural.predicate, "(至|到|~)".regex, and(isNotLatent, or(xGrain(Month), xGrain(Day), xGrain(Hour))).predicate),
    prod = {
      case (options, Token(_, vd: NumeralData) :: _ ::
        Token(Time, td2@TimeData(pred2: TimeDatePredicate, _, g2, _, _, _, _, _, _, _, _, _)) :: _ )=>
        val _from = vd.value.toInt
        val td1: Option[TimeData] = g2 match {
          case Month if 0 <= _from && _from <= 12 => td2.copy(timePred = pred2.copy(month = _from))
          case Day if 0 <= _from && _from <= 31 => td2.copy(timePred = pred2.copy(dayOfMonth = _from))
          case Hour if 0 <= _from && _from < 24 => td2.copy(timePred = pred2.copy(hour = (_from < 12, _from)))
          case _ => None
        }
        td1 match {
          case Some(x) => tt(interval(if (g2 != Hour) Closed else Open, x, td2, options.timeOptions.beforeEndOfInterval))
          case None => None
        }
    }
  )

  /**
    * 时间区间
    */
  val ruleInAInterval = Rule(
    name = "in a <duration>",
    pattern = List(isNotLatentDuration.predicate, "[之以]?内".regex),
    prod = tokens {
      case Token(Duration, DurationData(value, grain, _, _, _, _)) :: _ =>
        val (g, v) = grain match {
          case Month => (Day, value * 30)
          case Week => (Day, value * 7)
          case _ => (grain, value)
        }
        tt(cycleN(notImmediate = false, g, v))
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
      (td1.timePred, td2.timePred) match {
        case (_: TimeDatePredicate, _: TimeDatePredicate) => None
        case _ =>
          if (td1.timeGrain >= td2.timeGrain) {
            val cal = calendar(td1, td2)
            if (td1.timeGrain > td2.timeGrain && (td1.timePred.isInstanceOf[SequencePredicate] || recentHint(td2.hint))) {
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
    prod = optTokens {
      case (options, Token(_, td1: TimeData) :: _ :: Token(_, td2: TimeData) :: _) if options.timeOptions.sequence =>
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
    prod = optTokens {
      case (options, Token(_, td1: TimeData) :: Token(_, td2: TimeData) :: _)
          if options.timeOptions.sequence && (td1.timeGrain > td2.timeGrain ||
            td1.timeGrain == td2.timeGrain && !(recentHint(td1.hint) && recentHint(td2.hint))) =>
        sequenceProd(td1, td2)
    }
  )

  /**
    * 明天之后的三天
    */
  val ruleSequence3 = Rule(
    name = "sequence3: <recent nominal> <duration>",
    pattern =
      List(isHint(RecentNominal).predicate, "(往|向|之)?(前|后)的?".regex, isNotLatentDuration.predicate),
    prod = optTokens {
      case (options, Token(_, td1: TimeData) :: Token(_, GroupMatch(_ :: s :: _)) ::
        Token(_, DurationData(value, grain, latent, _, _, _)) :: _)
        if options.timeOptions.sequence && td1.timeGrain == grain =>
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
    ruleNthTimeOfOrdinalGrain,
    ruleNthTimeOfOrdinalGrain2,
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
    ruleTimeBeforeOfAfter,
    ruleTimeBeforeOfAfter2,
    ruleTimeBeforeOfAfter3,
    ruleTimeAfterDuration,
    ruleTimeBeforeDuration,
    ruleFromToIntervalAbbr1
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
