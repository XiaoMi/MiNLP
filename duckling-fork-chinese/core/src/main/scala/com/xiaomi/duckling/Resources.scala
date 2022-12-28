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

package com.xiaomi.duckling

import java.io._
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import scala.collection.JavaConverters._
import scala.io.{Codec, Source}

import org.apache.commons.io.FileUtils

import com.typesafe.scalalogging.LazyLogging

object Resources extends LazyLogging {
  private def url(path: String): URL = {
    val base = Types.conf.getString("resources.remote")
    new URL("%s%s".format(base, path))
  }

  def readLinesFromUrl(path: String): List[String] = {
    val file = Paths.get(path)
    val enableCache = Types.conf.getBoolean("resources.enableCache")
    if (enableCache && Files.exists(file)) {
      Files.readAllLines(file).asScala.toList
    } else {
      val input = url(path).openStream()
      try {
        val lines = Source.fromInputStream(input)(Codec.UTF8).getLines().toList
        if (enableCache) Files.write(file, lines.asJava)
        lines
      } finally {
        input.close()
      }
    }
  }

  def readLines(resource: String): List[String] = {
    val input = tryResource(resource)
    try {
      Source.fromInputStream(input)(Codec.UTF8).getLines().toList
    } finally {
      input.close()
    }
  }

  def urlReader[B](resource: String)(f: Reader => B): B = {
    val file = Paths.get(resource)
    val enableCache = Types.conf.getBoolean("resources.enableCache")
    val stream =
      if (enableCache && Files.exists(file)) {
        logger.info(s"read $resource from cache")
        Files.newInputStream(file)
      } else {
        val link = url(resource)
        logger.info(s"read $resource from remote: $link")
        if (enableCache) FileUtils.copyURLToFile(link, new File(resource))
        link.openStream()
      }
    try {
      val reader = new InputStreamReader(stream)
      f(reader)
    } finally {
      stream.close()
    }
  }

  def reader[B](resource: String)(f: Reader => B): B = {
    val stream = tryResource(resource)
    try {
      val reader = new InputStreamReader(stream, StandardCharsets.UTF_8)
      f(reader)
    } finally {
      stream.close()
    }
  }

  def inputStream[B](resource: String)(f: InputStream => B): B = {
    val stream = tryResource(resource)
    try {
      f(stream)
    } finally {
      stream.close()
    }
  }

  def tryResource(resource: String): InputStream = {
    // explicit classloader for android
    // please reference to https://www.baeldung.com/java-class-vs-classloader-getresource for detailed info
    val classLoader: ClassLoader = getClass.getClassLoader
    val input = classLoader.getResourceAsStream(resource)
    if (input == null) throw new FileNotFoundException(s"Resource [$resource] not found")
    input
  }
}
