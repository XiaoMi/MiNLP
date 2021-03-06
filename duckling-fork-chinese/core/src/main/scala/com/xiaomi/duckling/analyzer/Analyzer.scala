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

package com.xiaomi.duckling.analyzer

import com.typesafe.scalalogging.LazyLogging

import com.xiaomi.duckling.Types.conf
import com.xiaomi.duckling.types.LanguageInfo

trait Analyzer {
  def analyze(sentence: String): LanguageInfo
}

object Analyzer extends LazyLogging {
  private val analyzer: Analyzer = {
    val className = conf.getString("analyzer")
    logger.info(s"pick up dims of: $className")
    Class.forName(className).getDeclaredConstructor().newInstance().asInstanceOf[Analyzer]
  }

  def analyze(sentence: String): LanguageInfo = {
    analyzer.analyze(sentence)
  }
}


