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

case object Repeat extends Dimension with Rules with Examples {
  override val name: String = "Repeat"

  override val dimDependents: List[Dimension] = List(TimeGrain, Duration, Time)
}

case class RepeatData(interval: DurationData,
                      n: Option[Int] = None,
                      start: Option[TimeData] = None)
    extends Resolvable {

  override def resolve(context: Context,
                       options: Options): Option[(ResolvedValue, Boolean)] = {
    val instant = start match {
      case Some(_start) =>
        _start.resolve(context, options) match {
          case Some((TimeValue(value, _, _, _, _, _), _)) =>
            Some(value)
          case _ => None
        }
      case None => None
    }
    Some(RepeatValue(interval, n, instant), false)
  }
}

case class RepeatValue(interval: DurationData,
                       n: Option[Int] = None,
                       start: Option[SingleTimeValue] = None)
    extends ResolvedValue {

  /**
   * 根据 ISO 8601, Repeat = R[n]/[interval form]/Duration
   * 其中 interval form = a/b, /b, a/,
   * @return
   */
  override def schema: Option[String] = {
    val duration =
      if (interval.schema.isEmpty) durationSchema(interval.value.toString, interval.grain).get
      else interval.schema.get
    start match {
      case Some(x) =>
        val (repr, grain) = x match {
          case IntervalValue(start, end) => (s"${start.datetime.toZonedDateTime()}/${end.datetime.toZonedDateTime()}", start.grain)
          case OpenIntervalValue(start, _) => (s"${start.datetime.toZonedDateTime()}", start.grain)
          case SimpleValue(instant) => (s"${instant.datetime.toZonedDateTime()}", instant.grain)
          case _ => ("undefined", Grain.NoGrain)
        }
        s"Repeat_${grain}_${repr}_$duration"
      case None => s"Repeat_$duration"
    }
  }
}
