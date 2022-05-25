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

import sbt._

object Dependencies {
  lazy val testDependencies =
    Seq(junit % Test, junitInterface % Test, scalaTest % Test, hamcrest % Test)

  lazy val coreDependencies = Seq(
    arm,
    scalaz,
    json4sJackson,
    commonslang,
    commonsText,
    commonsIO,
    guava,
    java8,
    lunar,
    jline,
    slf4jApi,
    scalaLogging,
    config,
    logback % Provided,
    emoji,
    trie
  ) ++ testDependencies

  lazy val serverDependencies = Seq(logback, spStarterWeb, spThymeleaf, reactor, lombok) ++ testDependencies

  lazy val benchmarkDependencies = Seq(scalaTest % Test, jmhAnn, jmhCore, slf4jnop)

  // nlp
  lazy val hanlp = "com.hankcs" % "hanlp" % "portable-1.7.8"

  // test
  lazy val junit = "junit" % "junit" % "4.12"
  lazy val hamcrest = "org.hamcrest" % "hamcrest" % "2.2"
  lazy val junitInterface = "com.novocode" % "junit-interface" % "0.11"
  lazy val scalatic = "org.scalactic" %% "scalactic" % "3.2.10"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.10"
  lazy val scalaMeter = "com.storm-enroute" %% "scalameter" % "0.18"
  lazy val jmhAnn = "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.21"
  lazy val jmhCore = "org.openjdk.jmh" % "jmh-core" % "1.21"

  lazy val json4sJackson = "org.json4s" %% "json4s-jackson" % "3.6.12"
  lazy val jackson = "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.4"
  // logger
  lazy val slf4jApi = "org.slf4j" % "slf4j-api" % "1.7.26"
  lazy val slf4jnop = "org.slf4j" % "slf4j-nop" % "1.7.26"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4"

  // utils
  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.27"
  lazy val arm = "com.michaelpollmeier" %% "scala-arm" % "2.1"
  lazy val commonslang = "org.apache.commons" % "commons-lang3" % "3.12.0"
  lazy val commonsText = "org.apache.commons" % "commons-text" % "1.9"
  lazy val commonsIO = "commons-io" % "commons-io" % "2.6"
  lazy val jline = "org.jline" % "jline" % "3.11.0"
  lazy val config = "com.typesafe" % "config" % "1.3.4"
  lazy val guava = "com.google.guava" % "guava" % "30.1.1-jre"
  lazy val lombok = "org.projectlombok" % "lombok" % "1.18.8" % Provided
  lazy val httpclient = "org.apache.httpcomponents" % "httpclient" % "4.5.6"
  lazy val java8 = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
  lazy val lunar = "com.github.heqiao2010" % "lunar" % "1.5"
  lazy val emoji = "com.vdurmont" % "emoji-java" % "5.1.1"
  lazy val trie = "com.hankcs" % "aho-corasick-double-array-trie" % "1.2.2"

  //web
  lazy val spThymeleaf = "org.springframework.boot" % "spring-boot-starter-thymeleaf" % "2.4.5"
  lazy val spStarterWeb = "org.springframework.boot" % "spring-boot-starter-web" % "2.4.5"
  lazy val reactor = "io.projectreactor" % "reactor-core" % "3.4.5"

  // http
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.1.8"
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % "2.5.19"
}
