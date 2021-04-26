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

package com.xiaomi.duckling.dimension.place

import com.google.common.collect.Sets

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods._
import com.xiaomi.duckling.dimension.matcher.{Phrase, PhraseMatch}
import com.xiaomi.duckling.dimension.place.Types._
import com.xiaomi.duckling.engine.PhraseLookup.PhraseMatcherFn

trait Rules extends DimRules {

  private val levels = Sets.newHashSet("省", "市", "州", "区", "县")

  val faultTolerantPlaceRecognize: PhraseMatcherFn = (w: String) => {
    if (placeNames.contains(w)) true
    else if (w.length > 2) {
      // 兼容当阳县/当阳市 & 南阳人
      (levels.contains(w.substring(w.length - 1)) || w.endsWith("人")) &&
        placeNames.contains(w.substring(0, w.length - 1))
    } else false
  }

  val rulePlace = Rule(
    name = "place: any",
    pattern = List(faultTolerantPlaceRecognize.phrase(1, 2)),
    prod = {
      case Token(PhraseMatch, Phrase(w)) :: _ =>
        val c1 = getPlaceByName(w)
        val (candidates, isBirth) =
          if (c1.nonEmpty) (c1, false)
          else (getPlaceByName(w.substring(0, w.length - 1)), w.endsWith("人"))
        Token(Place, PlaceData(candidates, isBirth, texts = toTexts(candidates)))
    }
  )

  val rulePlaceLevel =
    Rule(
      name = "place: place + 省/市/州",
      pattern = List(isPlaceLevel1.predicate, levels.dict),
      prod = {
        case Token(Place, PlaceData(candidates, isBirthPlace, _, _)) ::
          Token(PhraseMatch, Phrase(level)) :: _ =>
          if (isBirthPlace) None
          else {
            val left = candidates.filter { one =>
              if (one.name.endsWith(level)) true
              else
                one.category match {
                  case "省/州" => level == "省"
                  case "城市" => level == "市"
                  case "区县" => level == "区" || level == "县"
                  case _ => false
                }
            }
            if (left.nonEmpty) Token(Place, PlaceData(left, texts = toTexts(candidates)))
            else None
          }
      }
    )

  val rulePlaceMerge =
    Rule(
      name = "place: merge",
      pattern = List(isPlace.predicate, isPlace.predicate),
      prod = {
        case Token(_, PlaceData(c1, i1, l1, _)) :: Token(_, PlaceData(c2, i2, l2, _)) :: _ if l1 >= l2 =>
          if (!i1) {
            val list = merge(c1, c2)
            if (list.isEmpty) None
            else Token(Place, PlaceData(list, i2, l1 + l2, texts = toTexts(list)))
          } else None
      }
    )
}
