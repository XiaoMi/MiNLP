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

package duckling.dimension

import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.numeral._

package object rating {
  def token(interval: IntervalValue): Token = Token(Rating, RatingData(interval))

  val isConcreteRating: Predicate = {
    case Token(Rating, RatingData(NumeralValue(_, _))) => true
  }

  val isDoubleSideRating: Predicate = {
    case Token(Rating, RatingData(_: DoubleSideIntervalValue)) => true
  }

  def valueOfRating(data: Resolvable): Option[Double] = {
    data match {
      case RatingData(NumeralValue(n, _))    => n
      case NumeralData(value, _, _, _, _, _) => value
      case _                              => None
    }
  }
}
