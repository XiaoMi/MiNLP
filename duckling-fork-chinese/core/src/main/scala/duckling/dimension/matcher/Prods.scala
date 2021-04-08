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

package duckling.dimension.matcher

import java.util.{Set => JSet}

import duckling.Types.{ItemPhrase, Production, Token}
import duckling.engine.PhraseLookup.PhraseMatcherFn

object Prods {

  type TextsTokenPF = PartialFunction[List[String], Option[Token]]
  type TextTokenPF = PartialFunction[String, Option[Token]]

  /**
    * group(0): 匹配的原始串
    * group(1): 第一个分组
    */
  val regexMatch: TextsTokenPF => Production = (f: TextsTokenPF) => {
    case tokens: List[Token] =>
      tokens.headOption.flatMap {
        case Token(RegexMatch, GroupMatch(patterns)) => f(patterns) orElse None
        case _                                       => None
      }
  }

  /**
    * 只处理整串
    */
  val singleRegexMatch: TextTokenPF => Production = (f: TextTokenPF) => {
    regexMatch { case head :: _ => f(head) }
  }

  implicit class DicWrapper(words: JSet[String]) {

    private val f = (w: String) => words.contains(w)

    def dict: ItemPhrase = ItemPhrase(f, 1, 1)
  }

  implicit class TokenWrapper(fn: PhraseMatcherFn) {
    def token: ItemPhrase = ItemPhrase(fn, 1, 1)

    def phrase(min: Int, max: Int) = ItemPhrase(fn, min, max)
  }

  /**
    * 仅有词典匹配
    */
  val dictMatch: TextTokenPF => Production = (f: TextTokenPF) => {
    case Token(PhraseMatch, Phrase(text)) :: _ => f(text)
  }
}
