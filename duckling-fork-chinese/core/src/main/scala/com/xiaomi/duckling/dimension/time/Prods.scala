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

import com.xiaomi.duckling.Types.{Predicate => _, _}
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.numeral.Predicates._
import com.xiaomi.duckling.dimension.time.enums.Grain
import com.xiaomi.duckling.dimension.time.helper.TimeDataHelpers._
import com.xiaomi.duckling.dimension.time.predicates.TimeDatePredicate

object Prods {

  def tt(timeData: TimeData): Token = Token(Time, timeData)

  def tt(opt: Option[TimeData]): Option[Token] = opt.map(td => Token(Time, td))

  val tokenize = (timeData: TimeData) => Token(Time, timeData)

  val intersectDOM: (TimeData, Token) => Option[TimeData] = (td: TimeData, token: Token) => {
    getIntValue(token).flatMap { n =>
      val dayTd =
        TimeData(timePred = TimeDatePredicate(dayOfMonth = n.toInt), timeGrain = Grain.Day)
      intersect(dayTd, td)
    }
  }
}
