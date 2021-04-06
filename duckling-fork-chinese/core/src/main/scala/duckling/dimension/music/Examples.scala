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

package duckling.dimension.music

import scala.collection.immutable.SortedMap

import duckling.dimension.DimExamples
import duckling.Types

trait Examples extends DimExamples {

  implicit def toList[T](t: T): List[T] = t :: Nil

  val list: List[(Map[String, List[String]], List[String])] =
    List(
      (SortedMap("作曲" -> "刘德华"), "作曲:刘德华"),
      (SortedMap("编曲" -> "刘德华", "作曲" -> "刘德华"), "作曲-编曲:刘德华"),
      (SortedMap("作曲" -> "张三", "作词" -> "李四"), "作曲:张三 作词:李四"),
      (SortedMap("编曲" -> "墨辞", "作词" -> "刀郎"), "编曲:墨辞  词: 刀郎"),
      (SortedMap("编曲" -> "墨辞", "作词" -> "刀郎", "作曲" -> "周杰伦"), "编曲:墨辞  词: 刀郎 曲 周杰伦"),
      (SortedMap("作词" -> "爱内里菜", "作曲" -> "corin", "编曲" -> "corin"), "作词:爱内里菜/作曲/编曲:corin"),
      (SortedMap("作曲" -> "黄耀光", "作词" -> "林夕"), "作曲:黄耀光|填词:林夕")
    )

  override def pairs: List[(Types.ResolvedValue, List[String])] = list.map {
    case (expected, texts) => (LyricData(expected), texts)
  }
}
