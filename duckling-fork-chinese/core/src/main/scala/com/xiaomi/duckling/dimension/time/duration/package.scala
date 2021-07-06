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

import com.xiaomi.duckling.Types.{Predicate, Token}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.enums.Grain._
import com.xiaomi.duckling.dimension.time.grain._

package object duration {

  def tt(v: Int, g: Grain): Token = Token(Duration, DurationData(v, g, schema = durationSchema(v, g)))

  /**
    * Convert a duration to the given grain, rounded to the
    * nearest integer. For example, 1 month is converted to 4 weeks.
    */
  def withGrain(g: Grain, d: DurationData): DurationData = {
    if (g == d.grain) d
    else {
      val v = math.round(1.0f * inSeconds(d.grain, d.value) / inSeconds(g, 1))
      DurationData(v, g, schema = d.schema)
    }
  }

  def timesOneAndAHalf(grain: Grain, n: Int): Option[DurationData] = {
    var value = 60 * n + 30

    grain match {
      case Minute =>
        DurationData(value, Second, schema = durationSchema(value, Second))
      case Hour =>
        DurationData(value, Minute, schema = durationSchema(value, Minute))
      case Day =>
        value = 24 * n + 12
        DurationData(value, Hour, schema = durationSchema(value, Hour))
      case Month =>
        value = 30 * n + 15
        DurationData(value, Day, schema = durationSchema(value, Day))
      case Year =>
        value = 12 * n + 6
        DurationData(value, Month, schema = durationSchema(value, Month))
      case _ => None
    }
  }

  def isADecade: Predicate = {
    case t1: Token => getIntValue(t1).exists(num => num < 100 && num % 10 == 0)
  }

  def isMonth: Predicate = {
    case Token(TimeGrain, GrainData(Month, _)) => true
  }

  def isNotLatentGrain: Predicate = {
    case Token(TimeGrain, GrainData(_, latent)) => !latent
  }

  private def getShortGrain(grain: Grain): String = {
    grain match {
      case Quarter => "Q"
      case Year => "Y"
      case Month => "M"
      case Week => "W"
      case Day => "D"
      case Hour => "H"
      case Minute => "M"
      case Second => "S"
      case _ => throw new IllegalArgumentException("unsupported Grain")
    }
  }

  def durationSchema(value: Int, grain: Grain): Option[String] = {
    if (getShortGrain(grain).isEmpty) None
    else if (grain < Day) Some(s"PT$value${getShortGrain(grain)}")
    else Some(s"P$value${getShortGrain(grain)}")
  }

  def durationSchema(d1: DurationData, d2: DurationData): Option[String] = {
    if (d1.schema.isEmpty) d2.schema
    else if (d2.schema.isEmpty) d1.schema
    else {
      if (d1.schema.get.contains("T")) {
        Some(s"${d1.schema.get}${d2.schema.get.replace("PT", "")}")
      } else {
        Some(s"${d1.schema.get}${d2.schema.get.replace("P", "")}")
      }
    }
  }
}
