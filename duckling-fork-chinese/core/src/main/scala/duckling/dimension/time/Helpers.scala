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

package duckling.dimension.time

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.time.enums.Grain
import duckling.dimension.time.enums.Grain._
import duckling.dimension.time.form.{Month => _}
import duckling.dimension.time.predicates.{TimeDatePredicate, TimePredicate}

object Helpers {

  def mkSingleRegexRule(name: String, pattern: String, token: Option[Token]): Rule = {
    Rule(name, pattern = List(pattern.regex), prod = {
      case _ => token
    })
  }

  def maxPredicateGrain(pred: TimePredicate): Option[Grain] = {
    pred match {
      case TimeDatePredicate(second, minute, hour, _, dayOfWeek, dayOfMonth, month, year, _) =>
        if (year.nonEmpty) Year
        else if (month.nonEmpty) Month
        else if (dayOfWeek.nonEmpty || dayOfMonth.nonEmpty) Day
        else if (hour.nonEmpty) Hour
        else if (minute.nonEmpty) Minute
        else if (second.nonEmpty) Second
        else None
      case _ => None
    }
  }
}
