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

package com.xiaomi.duckling.dimension.url

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.Dimension
import com.xiaomi.duckling.dimension.implicits._

case object DuckURL extends Dimension with Rules with Examples {
  override val name: String = "URL"
}

case class UrlData(url: String, domain: String, protocol: Option[String] = None) extends Resolvable with ResolvedValue {
  override def resolve(context: Context, options: Options): Option[(ResolvedValue, Boolean)] = {
    (this, false)
  }
}
