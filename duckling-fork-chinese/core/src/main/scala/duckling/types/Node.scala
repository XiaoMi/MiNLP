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

package duckling.types

import duckling.{Document, Types}
import duckling.Types._

case class Node(range: Types.Range,
                token: Token,
                children: List[Node],
                rule: Option[String],
                production: Production,
                features: Extraction = emptyExtraction) {

  def isValid(doc: Document): Boolean = token.dim.constraints(doc, this)

  def length: Int = range.length

  override def toString: String = {
    val sc = if (children.isEmpty) "[]" else children.mkString("[", ", ", "]")
    s"""{range = [${range.start}, ${range.end}),
       |token = $token,
       |children = $sc,
       |rule = ${rule.getOrElse("_")}}""".stripMargin
  }
}