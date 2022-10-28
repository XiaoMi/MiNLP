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
  /**
    * generate duration token
    * @param v  duration value
    * @param g  duration grain
    * @return
    */
  def tt(v: Int, g: Grain): Token = Token(Duration, DurationData(v, g, schema = durationSchema(v.toString, g)))

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
    grain match {
      case Minute => DurationData(60 * n + 30, Second, schema = Some(s"PT${n}M30S"))
      case Hour => DurationData(60 * n + 30, Minute, schema = Some(s"PT${n}H30M"))
      case Day => DurationData(24 * n + 12, Hour, schema = Some(s"P${n}DT12H"))
      case Month => DurationData(30 * n + 15, Day, schema = Some(s"P${n}M15D"))
      case Year => DurationData(12 * n + 6, Month, schema = Some(s"P${n}Y6M"))
      case _ => None
    }
  }

  def isADecade: Predicate = {
    case t1: Token => getIntValue(t1).exists(num => num < 100 && num % 10 == 0)
  }

  def isMonth: Predicate = {
    case Token(TimeGrain, GrainData(Month, _)) => true
  }

  def isNotLatentDuration: Predicate = {
    case Token(Duration, DurationData(_, _, latent, _, _)) => !latent
  }

  def isFuzzyNotLatentDuration: Predicate = {
    case Token(Duration, DurationData(_, _, _, fuzzy, _)) => fuzzy
  }

  def isNotLatentGrain: Predicate = {
    case Token(TimeGrain, GrainData(_, latent)) => !latent
  }
  
  private def grainShorthand(grain: Grain): String = {
    grain match {
      case Quarter => "Q"
      case Year => "Y"
      case Month => "M"
      case Week => "W"
      case Day => "D"
      case Hour => "H"
      case Minute => "M"
      case Second => "S"
      case NoGrain => ""
      case _ => throw new IllegalArgumentException("unsupported Grain")
    }
  }
  
  def durationSchema(value: String, grain: Grain): Option[String] = {
    if (grainShorthand(grain).isEmpty) None
    else if (grain < Day) Some(s"PT$value${grainShorthand(grain)}")
    else Some(s"P$value${grainShorthand(grain)}")
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
