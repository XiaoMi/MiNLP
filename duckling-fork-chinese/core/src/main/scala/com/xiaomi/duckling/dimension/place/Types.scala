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

package com.xiaomi.duckling.dimension.place

import com.google.common.collect.{ImmutableListMultimap, Maps}
import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.Serialization.{read, writePretty}
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util
import java.util.{Set => JSet}

import scala.collection.JavaConverters._

import com.xiaomi.duckling.JsonSerde._
import com.xiaomi.duckling.Resources
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.engine.LexiconLookup.Dict

object Types extends LazyLogging {

  case class PlaceOne(id: Int,
                      category: String,
                      name: String,
                      alias: List[String],
                      code: String,
                      partOf: Int) {

    def isDescendantOf(o: PlaceOne, table: Map[Int, PlaceOne]): Boolean = {
      if (partOf <= 0) false
      else if (partOf == o.id) true
      else table(partOf).isDescendantOf(o, table)
    }

    def getPath(): util.List[PlaceOne] = {
      getAllParents(this)
    }

    def getPathStr(): String = {
      getAllParents(this).asScala.map(_.name).mkString("/")
    }
  }

  private lazy val placeById: Map[Int, PlaceOne] = Resources.reader("/places4.json") {
    in: Reader =>
      val ones = read[Seq[PlaceOne]](in).map(p => (p.id, p)).toMap
      logger.info(s"read ${ones.size} for Place")
      ones
  }

  val levelExcludes = Set("县", "市辖区", "省直辖县级行政区划")

  private val placeByName: ImmutableListMultimap[String, PlaceOne] = {
    val builder = ImmutableListMultimap.builder[String, PlaceOne]()
    placeById.values.foreach { o =>
      val names = o.alias :+ o.name
      names.foreach(s => builder.put(s, o))
    }
    builder.build()
  }

  val placeNames: JSet[String] = placeByName.keySet()

  val placeDict = {
    val tm = Maps.newTreeMap[String, String]()
    placeByName.keySet().asScala.diff(levelExcludes).foreach(k => tm.put(k, k))
    new Dict(tm, maximumOnly = true)
  }

  def getPlaceByName(w: String): List[PlaceOne] = {
    placeByName.get(w).asScala.toList
  }

  /**
    * 返回所有的父节点，包括自己
    */
  def getAllParents(one: PlaceOne): util.List[PlaceOne] = {
    Stream
      .iterate(one)(one => if (one.partOf > 0) placeById(one.partOf) else null)
      .takeWhile(_ != null)
      .toList
      .reverse
      .asJava
  }

  val isPlace: Predicate = isDimension(Place)

  val isPlaceLevel1: Predicate = {
    case Token(Place, PlaceData(_, _, level, _)) => level == 1
  }

  def merge(c1: List[PlaceOne], c2: List[PlaceOne]): List[PlaceOne] = {
    (for (a <- c1; b <- c2) yield {
      if (a.isDescendantOf(b, placeById)) Some(a)
      else if (b.isDescendantOf(a, placeById)) Some(b)
      else None
    }).flatten
  }

  /**
    * 加工示例
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {
    val places = placeById.values
      .map { one =>
        val alias = one.alias.filter(_.length > 1)
        one.copy(alias = alias)
      }
      .toList
      .sortBy(_.id)
    val writer = Files.newBufferedWriter(
      Paths.get("core/src/main/resources/dict/places4.json"),
      StandardCharsets.UTF_8
    )
    writePretty(places, writer)
    writer.close()
  }
}
