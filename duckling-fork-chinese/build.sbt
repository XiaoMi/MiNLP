import Dependencies._

// 配置大量参考了
// Scalaz [[https://github.com/scalaz/scalaz]]项目的配置
// 以及 https://github.com/olafurpg/sbt-ci-release

lazy val sharedSettings = Seq(
  organization := "com.xiaomi.duckling",
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.10"),
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.mavenLocal, // 加速
    Resolver.mavenCentral
  ),
  Test / parallelExecution := false,
  run / fork := true,
  // sbt run with provided scope
  Compile / run := Defaults
    .runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner)
    .evaluated,
  Compile / runMain := Defaults
    .runMainTask(Compile / fullClasspath, Compile / run / runner)
    .evaluated,

  scmInfo := Some(ScmInfo(url("https://github.com/XiaoMi/MiNLP"), "scm:git@github.com:XiaoMi/MiNLP.git")),
  homepage := Some(url("https://github.com/XiaoMi/MiNLP/tree/main/duckling-fork-chinese")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer("du00cs", "Ninglin Du", "du00cs@outlook.com", url("https://github.com/du00cs")),
    Developer("zhangsonglei", "Songlei Zhang", "zhangsonglei@xiaomi.com", url("https://github.com/zhangsonglei"))
  ),
  pomIncludeRepository := { _ => false },
  sonatypeProfileName := "com.xiaomi",
  Test / publishArtifact := false
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)

publish / skip := true // don't publish the root project

lazy val `duckling-fork-chinese` = project.in(file("."))
  .settings(sharedSettings)
  .aggregate(core, learning, test, lite, server, benchmark)

lazy val core = project
  .settings(
    name := "duckling-core",
    sharedSettings,
    libraryDependencies ++= coreDependencies
  ).dependsOn(test % "test->test")

lazy val test = project
  .settings(
    name := "duckling-test",
    sharedSettings,
    libraryDependencies ++= testDependencies
  )

lazy val learning = project
  .settings(
    name := "duckling-learning",
    sharedSettings,
    libraryDependencies ++= learningDependencies
  ).dependsOn(core % "compile->compile", test % "test->test")

lazy val lite = project
  .settings(
    name := "duckling-lite",
    sharedSettings,
    libraryDependencies ++= Seq(
      logback % Provided // logging
    ),
    excludeDependencies ++= liteExcludes
  ).dependsOn(core % "compile->compile", test % "test->test")

lazy val server = project
  .settings(
    name := "duckling-server",
    sharedSettings,
    libraryDependencies ++= serverDependencies,
    publish / skip := true,
    // 打包
    Universal / javaOptions ++= Seq("-J-Xmx1g"),
    scriptClasspath := Seq("../conf", "*")
  )
  .dependsOn(core, test % "test->test")
  .enablePlugins(JavaServerAppPackaging)

lazy val benchmark = project
  .settings(
    name := "duckling-benchmark",
    sharedSettings,
    libraryDependencies ++= benchmarkDependencies,
    publish / skip := true,
    outputStrategy := Some(StdoutOutput),
    fork := true,
    connectInput := true,
    logBuffered := false
  )
  .dependsOn(core)

// 不能正常工作，jline不能正常初始化，先留着
// 可以使用 sbt learning/console ，然后输入 com.xiaomi.duckling.task.NaiveBayesConsole.run() 来代替
lazy val duckConsole = taskKey[Unit]("duck console task")
duckConsole := (learning / Runtime / runMain).toTask(" com.xiaomi.duckling.task.NaiveBayesConsole").value

lazy val duckModel = taskKey[Unit]("duck naive bayes training task")
duckModel := (learning / Runtime / runMain).toTask(" com.xiaomi.duckling.task.Training").value
