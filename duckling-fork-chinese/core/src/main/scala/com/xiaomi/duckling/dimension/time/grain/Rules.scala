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

package com.xiaomi.duckling.dimension.time.grain

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.Prods.singleRegexMatch
import com.xiaomi.duckling.dimension.time.enums.Grain._

trait Rules extends DimRules {

  val grains = List(
    ("second (grain)", "秒钟?", Second),
    ("minute (grain)", "分(?!贝)钟?", Minute),
    ("hour (grain)", "(小时|钟头|钟|点)", Hour),
    ("day (grain)", "(天(?!气(?!温))|日(?![元本]))", Day),
    ("week (grain)", WeekPattern, Week),
    ("month (grain)", "月", Month),
    ("quarter (grain)", "季度", Quarter),
    ("year (grain)", "年", Year)
  )

  val latentExpr = Set("分", "钟", "点")

  override def dimRules: List[Rule] = grains.map {
    case (name, regexPattern, grain) =>
      Rule(name = name, pattern = List(regexPattern.regex), prod = singleRegexMatch {
        case s =>
          Token(TimeGrain, GrainData(grain, latentExpr.contains(s), s))
      })
  }
}
