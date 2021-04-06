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

package duckling.dimension.rating

import duckling.Types.{NumeralValue => _, _}
import duckling.dimension.matcher.GroupMatch
import duckling.dimension.numeral._
import duckling.dimension.time.enums.IntervalDirection._
import duckling.dimension.time.enums.IntervalType.Closed
import duckling.dimension.DimRules
import duckling.dimension.implicits._

trait Rules extends DimRules {

  val Grade = "(打分|评分|分数)"

  val concrete = Rule(
    name = "concrete rating <number>",
    pattern = List(isDimension(Numeral).predicate, "分(?!钟)".regex),
    prod = {
      case Token(_, NumeralData(value, _, _, _, _, _)) :: _ =>
        token(NumeralValue(value))
    }
  )

  val concrete2 = Rule(
    name = "concrete rating <number/rating>",
    pattern = List(s"${Grade}在?".regex, or(isConcreteRating, isDimension(Numeral)).predicate),
    prod = {
      case _ :: Token(_, data) :: _ =>
        val n = data match {
          case NumeralData(value, _, _, _, _, _) => NumeralValue(value)
          case RatingData(n: NumeralValue)    => n
        }
        token(n)
    }
  )

  val greater = Rule(
    name = "rating > |rating/number|",
    pattern =
      List(s"$Grade(大于|高于|超过|不低于)".regex, or(isConcreteRating, isDimension(Numeral)).predicate),
    prod = {
      case _ :: Token(_, data) :: _ =>
        val n = data match {
          case RatingData(NumeralValue(n, _))    => n
          case NumeralData(value, _, _, _, _, _) => value
        }
        token(OpenIntervalValue(n, After))
    }
  )

  val less = Rule(
    name = "rating < |rating|",
    pattern =
      List(s"$Grade(小于|低于|不超过|不高于)".regex, or(isConcreteRating, isDimension(Numeral)).predicate),
    prod = {
      case _ :: Token(_, data) :: _ =>
        val n = data match {
          case RatingData(NumeralValue(n, _))    => n
          case NumeralData(value, _, _, _, _, _) => value
        }
        token(OpenIntervalValue(n, Before))
    }
  )

  val ratingUpDown = Rule(
    name = "<rating> up/down",
    pattern = List(isConcreteRating.predicate, "以(上|下)".regex),
    prod = {
      case Token(_, RatingData(NumeralValue(n, _))) :: Token(_, GroupMatch(_ :: s :: _)) :: _ =>
        val direction = if (s == "上") After else Before
        token(OpenIntervalValue(n, direction))
    }
  )

  val ratingToRating = Rule(
    name = "<rating> to <rating>",
    pattern = List(
      or(isConcreteRating, isDimension(Numeral)).predicate,
      "(到|至)".regex,
      isConcreteRating.predicate
    ),
    prod = {
      case Token(_, d1) :: _ :: Token(_, d2) :: _ =>
        for {
          n1 <- valueOfRating(d1)
          n2 <- valueOfRating(d2)
        } yield {
          token(DoubleSideIntervalValue(n1, n2, rightType = Closed))
        }
    }
  )

  val ratingToRating2 = Rule(
    name = "rating <number> to <number>",
    pattern = List(
      s"${Grade}在?".regex,
      or(isConcreteRating, isDimension(Numeral)).predicate,
      "(到|至)".regex,
      or(isConcreteRating, isDimension(Numeral)).predicate
    ),
    prod = {
      case _ :: Token(_, d1) :: _ :: Token(_, d2) :: _ =>
        for {
          n1 <- valueOfRating(d1)
          n2 <- valueOfRating(d2)
        } yield {
          token(DoubleSideIntervalValue(n1, n2, rightType = Closed))
        }
    }
  )

  val rangeOfRatingsWithSuffix = Rule(
    name = "range of rating <suffix>",
    pattern = List(isDoubleSideRating.predicate, "之间".regex),
    prod = {
      case t1 :: _ => t1
    }
  )
}
