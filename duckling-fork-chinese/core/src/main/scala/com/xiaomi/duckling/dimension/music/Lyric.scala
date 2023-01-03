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

package com.xiaomi.duckling.dimension.music

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.Dimension

case object Lyric extends Dimension with Rules {
  override val name: String = "Lyric"
}

case class LyricData(roles: Map[String, List[String]]) extends Resolvable with ResolvedValue {

  def add(role: String, names: List[String]): LyricData = {
    val values =
      if (roles.contains(role)) (roles(role) ++ names).distinct
      else names
    copy(roles = roles.updated(role, values))
  }

  override def resolve(context: Context, options: Options): Option[(LyricData, Boolean)] = {
    Some(this, false)
  }
}
