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

case object Repeat extends Dimension with Rules {
  override val name: String = "Repeat"

  override val dimDependents: List[Dimension] = List(TimeGrain, Duration, Time)
}

case class RepeatData(interval: Option[DurationData] = None,
                      n: Option[Int] = None,
                      start: Option[TimeData] = None,
                      workdayType: Option[WorkdayType] = None)
    extends Resolvable {

  override def resolve(context: Context,
                       options: Options): Option[(ResolvedValue, Boolean)] = {
    val instant = start match {
      case Some(_start) =>
        _start.resolve(context, options) match {
          case Some((tv: TimeValue, _)) => Some(tv, _start.form)
          case _ => None
        }
      case None => None
    }
    Some(RepeatValue(interval, n, instant, workdayType), false)
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
                       workdayType: Option[WorkdayType] = None)
    extends ResolvedValue {

  /**
   * 参考了 ISO 8601, Repeat = R[n]/[interval form]/Duration
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
