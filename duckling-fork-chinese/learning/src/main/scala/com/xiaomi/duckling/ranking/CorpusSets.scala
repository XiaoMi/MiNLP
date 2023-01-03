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

package com.xiaomi.duckling.ranking

import scala.collection.JavaConverters.asScalaSetConverter

import org.reflections.Reflections

import com.typesafe.scalalogging.LazyLogging

import shapeless.Typeable
import shapeless.syntax.typeable.typeableOps

import com.xiaomi.duckling.Document
import com.xiaomi.duckling.Types._
import com.xiaomi.duckling.dimension.DimExamples

object CorpusSets extends LazyLogging {
  type Example = (Document, ResolvedValue)
  type Corpus = (Context, Options, List[Example])

  def examples(output: ResolvedValue,
               texts: List[String],
               enableAnalyzer: Boolean = false): List[Example] = {
    texts.map(text => (Document.fromText(text, enableAnalyzer = enableAnalyzer), output))
  }

  def findAllObjects[T](`package`: String, clazz: Class[T])(implicit t: Typeable[T]): Set[T] = {
    val reflections = new Reflections(`package`)
    reflections.getSubTypesOf(clazz).asScala
      .flatMap(c => c.getField("MODULE$").get(null).cast[T])
      .toSet
  }

  val dimExamples = CorpusSets.findAllObjects("com.xiaomi.duckling.dimension", classOf[DimExamples])
    .map(d => (d.dimension, d))
    .toMap
}
