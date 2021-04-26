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

package com.xiaomi.duckling.constraints

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types.{Range, ResolvedToken}
import com.xiaomi.duckling.types.LanguageInfo

/**
 * "下周日本的天气" 在给定语义范围 [下周/天门/的/天气]的情况下，可以避免解析出 [下周天]
 */
object TokenSpan extends Constraint with LazyLogging {
  override def isValid(lang: LanguageInfo, resolvedToken: ResolvedToken): Boolean = {
    val Range(ns, ne) = resolvedToken.range
    val dim = resolvedToken.node.token.dim
    if (lang.tokens.isEmpty) true
    else {
      val overlapLeft = lang.tokens.find(token => ns < token.`end`)
      val overlapRight = lang.tokens.find(token => ne <= token.`end`)
      (overlapLeft, overlapRight) match {
        case (Some(tokenL), Some(tokenR)) =>
          val bool = tokenL != tokenR && (ns > tokenL.start || ne < tokenR.`end`)
          if (bool) {
            logger.info(s"TokenSpan filter: ${dim} => [${tokenL.word}/${tokenR.word}] & ${lang.sentence.substring(ns, ne)}")
          }
          !bool
        case _ => false // 正确生成的lang不会到这里
      }
    }
  }
}
