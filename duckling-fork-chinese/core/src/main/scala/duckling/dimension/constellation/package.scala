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

package duckling.dimension

import java.io.Reader

import org.json4s.jackson.Serialization.read

import duckling.JsonSerde._
import duckling.Resources

package object constellation {
  case class Lexicon(lexeme: List[String], target: String)

  lazy val lexicons = Resources.reader("constellation.json") { in: Reader =>
    read[Seq[Lexicon]](in)
  }
}
