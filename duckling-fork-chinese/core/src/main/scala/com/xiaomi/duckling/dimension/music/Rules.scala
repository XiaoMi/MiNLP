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

import scalaz.Scalaz._

import scala.collection.immutable.SortedMap
import scala.util.matching.Regex

import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimRules
import com.xiaomi.duckling.dimension.implicits._
import com.xiaomi.duckling.dimension.matcher.{GroupMatch, Varchar}

trait Rules extends DimRules {
  private implicit def toList[T](t: T): List[T] = t :: Nil

  private val NameMap: Map[String, List[String]] = Map(
    "曲" -> "作曲",
    "词" -> "作词",
    "填词" -> "作词",
    "编" -> "编曲",
    "曲词" -> List("作曲", "作词"),
    "词曲" -> List("作曲", "作词"),
    "作曲编曲" -> List("作曲", "编曲"),
    "曲编" -> List("作曲", "编曲"),
    "作编曲" -> List("作曲", "编曲"),
    "composer" -> "作曲",
    "lyricist" -> "作词",
    "arranger" -> "编曲",
    "arrangement" -> "编曲"
  )

  private val splitPattern = "(\\s*[/、·.&\\\\-]+\\s*)"

  private val colon = "[:;]"

  private val basePattern =
    "((Lyricist|Composer|Arranger|Arrangement|作曲编曲|弦乐编写|和声|制作人|键盘|作编曲|填词|作\\s*词|作\\s*曲|编\\s*曲|原曲|曲词|曲编|词曲|混音|录音|rap词|吉他)|((?=^|[^作编]|\\W)(词|曲|唱|编|歌)))"

  val RolePattern = s"(?i)$basePattern($splitPattern$basePattern)*"

  def norm(s: String): List[String] = {
    s.split(splitPattern)
      .map(_.replaceAll("\\s+", ""))
      .flatMap(t => NameMap.getOrElse(t.toLowerCase(), List(t)))
      .toList
  }

  val Excludes
    : List[Regex] = DefaultExcludes :+ colon.r :+ s"(?i)$basePattern".r :+ s"\\|".r :+ splitPattern.r

  val explicitRule = Rule(
    name = "Lyric:Explicit",
    pattern = RolePattern.regex :: colon.regex :: (2, 50, Excludes).varchar :: Nil,
    prod = {
      case Token(_, GroupMatch(role :: _)) :: _ :: Token(_, Varchar(s, _)) :: Nil =>
        val names = s.split("[/,、]").map(_.trim).toList
        // 使用SortedMap保证Json序列化时不会出现顺序不一致
        val m = SortedMap(norm(role).map((_, names)): _*)
        Token(Lyric, LyricData(m))
    }
  )

  val implicitRule = Rule(
    name = "Lyric:Implicit",
    pattern = RolePattern.regex :: (2, 50, Excludes).varchar :: Nil,
    prod = {
      case Token(_, GroupMatch(role :: _)) :: Token(_, Varchar(s, _)) :: Nil =>
        val names = s.split("[/,、]").map(_.trim).toList
        // 使用SortedMap保证Json序列化时不会出现顺序不一致
        val m = SortedMap(norm(role).map((_, names)): _*)
        Token(Lyric, LyricData(m))
    }
  )

  val compose0 = Rule(
    name = "Lyric:Compose",
    pattern = List(isDimension(Lyric).predicate, isDimension(Lyric).predicate),
    prod = {
      case Token(_, LyricData(m1)) :: Token(_, LyricData(m2)) :: _ =>
        Token(Lyric, LyricData(m1 |+| m2))
    }
  )

  val compose1 = Rule(
    name = "Lyric:Compose with split",
    pattern = List(isDimension(Lyric).predicate, "[|/]+".regex, isDimension(Lyric).predicate),
    prod = {
      case Token(_, LyricData(m1)) :: _ :: Token(_, LyricData(m2)) :: _ =>
        Token(Lyric, LyricData(m1 |+| m2))
    }
  )
}
