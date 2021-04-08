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

package duckling

import java.time.ZonedDateTime
import java.util.Locale

import org.json4s.jackson.Serialization.write

import com.typesafe.config.ConfigFactory

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}

import duckling.JsonSerde._
import duckling.Types._
import duckling.dimension.implicits._
import duckling.dimension.CorpusSets.namedDimensions
import duckling.ranking.Ranker

object WebServer extends HttpApp {
  private val option = Options(entityWithNode = true, withLatent = false)

  val template = Resources.readLines("/template.html").mkString("\n")

  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.load()
    WebServer.startServer(conf.getString("host"), conf.getInt("port"))
  }

  def parse(dims: Array[String], rawInput: String): List[Answer] = {
    val context = Context(ZonedDateTime.now(), Locale.CHINA)
    val sentence = rawInput.trim // 有条件最好做一下预处理
    val targets = dims.map(namedDimensions).toSet
    val ranker: Ranker = Ranker.NaiveBayes
    val options = option.copy(
      targets = targets,
      rankOptions = RankOptions(ranker = Some(ranker), combinationRank = true),
      full = false,
      withLatent = false
    )
    Api.analyze(sentence, context, options)
  }

  def page(rawInput: String, answers: List[Answer]): HttpEntity.Strict = {
    HttpEntity(ContentTypes.`text/html(UTF-8)`, TokenVisualization.toHtml(rawInput, answers))
  }

  def json(rawInput: String, answers: List[Answer]): HttpEntity.Strict = {
    HttpEntity(ContentTypes.`application/json`, write(answers))
  }

  def request(f: (String, List[Answer]) => HttpEntity.Strict): Route = {
    parameter("dim", "query") { (dim, rawInput) =>
      val dims = dim.toLowerCase().split(",").map(_.trim)
      val unknownDims = dims.filter(d => !namedDimensions.contains(d))
      if (unknownDims.length == 0) {
        complete {
          val answers = parse(dims, rawInput)
          f(rawInput, answers)
        }
      } else {
        failWith(new IllegalArgumentException(s"unsupported dim: ${unknownDims.mkString(", ")}"))
      }
    }
  }

  def place: Route = {
    parameter("query") { query =>
      val s = PlaceQuery.extract(query).getOrElse("")
      complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s))
    }
  }

  override protected def routes: Route =
    path("duckling") {
      get {
        // web页面
        request(page)
      }
    } ~
      (get & pathPrefix("static")) {
        getFromResourceDirectory("static")
      } ~ path("api") {
      get {
        request(json)
      }
    } ~ path("api" / "place") {
      get {
        place
      }
    }

}
