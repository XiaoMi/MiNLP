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
import com.xiaomi.duckling.dimension.matcher.{LexiconMatch, LexiconMatches, Phrase, PhraseMatch}
import com.xiaomi.duckling.dimension.place.Types._
import com.xiaomi.duckling.engine.PhraseLookup.PhraseMatcherFn

trait Rules extends DimRules {

  private val levels = Sets.newHashSet("省", "市", "州", "区", "县")

  val rulePlace = Rule(
    name = "place: any",
    pattern = List(placeDict.lexicon),
    prod = tokens {
      case Token(LexiconMatch, LexiconMatches(s, t)) :: _ =>
        val candidates = getPlaceByName(s)
        Token(Place, PlaceData(candidates, texts = toTexts(candidates)))
    }
  )

  val rulePlaceBirth = Rule(
    name = "place: any",
    pattern = List(isPlace.predicate, "人".regex),
    prod = tokens {
      case Token(Place, pd: PlaceData) :: _ =>
        Token(Place, pd.copy(isBirthPlace = true))
    }
  )

  val rulePlaceLevel =
    Rule(
      name = "place: place + 省/市/州",
      pattern = List(isPlaceLevel1.predicate, levels.dict),
      prod = tokens {
        case Token(Place, PlaceData(candidates, false, _, _)) ::
          Token(PhraseMatch, Phrase(level)) :: _ =>
            val left = candidates.filter { one =>
              if (one.name.endsWith(level)) true
              else one.category match {
                  case "省/州" => level == "省"
                  case "城市" => level == "市" || level == "县" // 市县错误时，模糊一下
                  case "区县" => level == "区" || level == "县"
                  case _ => false
                }
            }
            if (left.nonEmpty) Token(Place, PlaceData(left, texts = toTexts(candidates)))
            else None
      }
    )

  val rulePlaceMerge =
    Rule(
      name = "place: merge",
      pattern = List(isPlace.predicate, isPlace.predicate),
      prod = tokens {
        case Token(_, PlaceData(c1, false, l1, _)) :: Token(_, PlaceData(c2, i2, l2, _)) :: _ if l1 >= l2 =>
          val list = merge(c1, c2)
          if (list.isEmpty) None
          else Token(Place, PlaceData(list, i2, l1 + l2, texts = toTexts(list)))
      }
    )
}
