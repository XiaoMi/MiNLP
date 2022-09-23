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

  def tt(v: Int, g: Grain): Token = Token(Duration, DurationData(v, g))

  /**
    * Convert a duration to the given grain, rounded to the
    * nearest integer. For example, 1 month is converted to 4 weeks.
    */
  def withGrain(g: Grain, d: DurationData): DurationData = {
    if (g == d.grain) d
    else {
      val v = math.round(1.0f * inSeconds(d.grain, d.value) / inSeconds(g, 1))
      DurationData(v, g)
    }
  }

  def timesOneAndAHalf(grain: Grain, n: Int): Option[DurationData] = {
    grain match {
      case Minute => DurationData(60 * n + 30, Second)
      case Hour => DurationData(60 * n + 30, Minute)
      case Day => DurationData(24 * n + 12, Hour)
      case Month => DurationData(30 * n + 15, Day)
      case Year => DurationData(12 * n + 6, Month)
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
    case Token(Duration, DurationData(_, _, latent, _)) => !latent
  }

  def isFuzzyNotLatentDuration: Predicate = {
    case Token(Duration, DurationData(_, _, _, fuzzy)) => fuzzy
  }

  def isNotLatentGrain: Predicate = {
    case Token(TimeGrain, GrainData(_, latent)) => !latent
  }
}
