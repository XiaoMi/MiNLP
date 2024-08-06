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

package com.xiaomi.duckling.dimension.time.repeat

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.time.{Time, TimeData, TimeValue}
import com.xiaomi.duckling.dimension.time.duration.{durationSchema, Duration, DurationData}
import com.xiaomi.duckling.dimension.time.grain.TimeGrain
import com.xiaomi.duckling.dimension.time.Types._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.form.Form
import com.xiaomi.duckling.dimension.time.Helpers.countGrains

case object Repeat extends Dimension with Rules {
  override val name: String = "Repeat"

  override val dimDependents: List[Dimension] = List(TimeGrain, Duration, Time)
}

case class RepeatData(interval: Option[DurationData] = None, // 间隔，如果与其它的配合，表示外层间隔
                      n: Option[Int] = None,
                      start: Option[TimeData] = None,
                      workdayType: Option[WorkdayType] = None,
                      repeatGrain: Option[Grain] = None,
                      repeatNFromInterval: Option[TimeData] = None)
    extends Resolvable {

  override def resolve(context: Context,
                       options: Options): Option[(ResolvedValue, Boolean)] = {
    val (instant, success) = start match {
      case Some(_start) =>
        _start.resolve(context, options) match {
          case Some((tv: TimeValue, _)) => (Some(tv, _start.form), true)
          case _ => (None, false)
        }
      case None => (None, true)
    }
    val repeatN = repeatNFromInterval match {
      case Some(intervalTimeData) =>
        intervalTimeData.resolve(context, options) match {
          case Some((tv: TimeValue, _)) =>
            tv.timeValue match {
              case IntervalValue(start, end) => Some(countGrains(start, end))
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
    if (success) Some(RepeatValue(interval, n.orElse(repeatN), instant, repeatGrain = repeatNFromInterval.map(_.timeGrain), workdayType), false)
    else None
  }
}

/**
 * repeat bean
 * @param interval 重复间隔，来自"每天/周"这部分
 * @param n 重复次数，"接下来三天"
 * @param start 初始时间或区间，比如 8点或者8-10点，在workdayType生效时，date部分需要忽略
 * @param workdayType 工作日/非工作日，是一种重复类型
 */
case class RepeatValue(interval: Option[DurationData] = None,
                       n: Option[Int] = None,
                       start:  Option[(TimeValue, Option[Form])] = None,
                       repeatGrain: Option[Grain] = None,
                       workdayType: Option[WorkdayType] = None)
    extends ResolvedValue {

  /**
   * 参考了 ISO 8601, Repeat = R[n]/[interval form]/Duration
   * 扩展为: Repeat_[time/interval grain 时间表达本身的粒度]_[time/interval 时间或区间表达]_{outer period 外层重复的间隔}_{n x inner_period 内层循环的次数和粒度}
   * inner_period = 'n' inner repeat grain
   * 比如每周三到周五的早上 => Repeat_Hour_2024-08-07T08:00:00/2024-08-07T12:00:00_P1W_3P1D
   * 其中 interval form = a/b, /b, a/,
   * @return
   */
  override def schema: Option[String] = {
    val duration =
      if (interval.nonEmpty) {
        val _interval = interval.get
        if (_interval.schema.isEmpty) durationSchema(_interval.value.toString, _interval.grain).get
        else _interval.schema.get
      } else if (workdayType.nonEmpty) {
        workdayType.get.toString
      } else "undefined"
    val isWorkdayType = workdayType.nonEmpty
    start match {
      case Some((TimeValue(x, _, _, _, _, _), _)) =>
        val (repr, grain) = x match {
          case IntervalValue(start, end) => (s"${format(start.datetime, isWorkdayType)}/${format(end.datetime, isWorkdayType)}", start.grain)
          case OpenIntervalValue(start, _) => (s"${format(start.datetime, isWorkdayType)}", start.grain)
          case SimpleValue(instant) => (s"${format(instant.datetime, isWorkdayType)}", instant.grain)
          case _ => ("undefined", Grain.NoGrain)
        }
        s"Repeat_${grain}_${repr}_$duration"
      case None => s"Repeat_$duration"
    }
  }

  private def format(dt: DuckDateTime, isTime: Boolean) : String = {
    if (!isTime) dt.toLocalDatetime.toString
    else dt.time.toString
  }
}
