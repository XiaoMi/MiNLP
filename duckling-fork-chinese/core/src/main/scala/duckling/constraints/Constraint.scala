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

package duckling.constraints

import duckling.Types.ResolvedToken
import duckling.types.LanguageInfo

/**
 * 约束，用于去掉一些解析结果，比如跨越了词边界的（切词是一种）
 */
trait Constraint {
  def isValid(lang: LanguageInfo, resolvedToken: ResolvedToken): Boolean
}
