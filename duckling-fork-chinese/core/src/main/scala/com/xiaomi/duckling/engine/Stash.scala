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

package com.xiaomi.duckling.engine

import scala.collection.SortedMap

import com.xiaomi.duckling.engine
import com.xiaomi.duckling.types.Node

case class Stash(getSet: SortedMap[Int, Set[Node]]) {
  def filter(p: Node => Boolean): Stash = engine.Stash(getSet.map { case (k, v) => (k, v filter p) })

  def isEmpty: Boolean = getSet.isEmpty

  def union(o: Stash): Stash = {
    val set = getSet ++ o.getSet.map {
      case (k, v) => k -> v.union(getSet.getOrElse(k, Set()))
    }
    Stash(set)
  }

  def toPosOrderedList(): List[Node] = {
    getSet.values.toList.flatMap(_.toList)
  }

  def toPosOrderedListFrom(pos: Int): List[Node] = {
    getSet.filter(_._1 >= pos).values.flatMap(_.toList).toList
  }
}

object Stash {
  def empty(): Stash = Stash(SortedMap())

  def fromList(ns: List[Node]): Stash = {
    val mkKV = (node: Node) => (node.range.start, Set(node))
    val map = (ns map mkKV).groupBy(_._1).mapValues(_.map(_._2).reduce(_ ++ _)).toList
    Stash(SortedMap(map: _*))
  }
}
