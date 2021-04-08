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

import duckling.dimension.time.enums.Grain
import duckling.dimension.time.enums.Grain._

package object grain {

  val WeekPattern = "(周|礼拜|星期)((?![一二三四五六天几])|(?=(天气|天津|天长|天水|天门)))"

  def inSeconds(g: Grain, n: Int): Int = {
    g match {
      case NoGrain => n
      case Second  => n
      case Minute  => n * 60
      case Hour    => n * inSeconds(Minute, 60)
      case Day     => n * inSeconds(Hour, 24)
      case Week    => n * inSeconds(Day, 7)
      case Month   => n * inSeconds(Day, 30)
      case Quarter => n * inSeconds(Month, 3)
      case Year    => n * inSeconds(Day, 365)
    }
  }
}
