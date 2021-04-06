import Dependencies._
import com.jsuereth.sbtpgp.SbtPgp.autoImport.PgpKeys.{publishLocalSigned, publishSigned}
import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import sbtrelease.Utilities._
import xerial.sbt.Sonatype.autoImport._

// 大量参考了 Scalaz [[https://github.com/scalaz/scalaz]]项目的配置

lazy val sharedSettings = Seq(
  organization := "duckling",
  scalaVersion := "2.11.12",
  // cross scala versions 还需要摸索
  // crossScalaVersions := Seq("2.11.12", "2.12.12"),
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    Resolver.mavenLocal, // 加速
    Resolver.typesafeIvyRepo("releases"), // 插件
    Resolver.mavenCentral
  ),
  Test / parallelExecution := false,
  run / fork := true,
  Compile / coverageEnabled := false,
  Test / coverageEnabled := true,
  // sbt run with provided scope
  Compile / run := Defaults
    .runTask(Compile / fullClasspath, Compile / run / mainClass, Compile / run / runner)
    .evaluated,
  Compile / runMain := Defaults
    .runMainTask(Compile / fullClasspath, Compile / run / runner)
    .evaluated,

  // 远程仓库发布
  pomExtra := <url>https://github.com/XiaoMi/MiNLP</url>
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
        <comments>A business-friendly OSS license</comments>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>du00cs</id>
        <name>Ninglin Du</name>
        <url>https://github.com/du00cs</url>
      </developer>
      <developer>
        <id>zhangsonglei</id>
        <name>Songlei Zhang</name>
        <url>https://github.com/zhangsonglei</url>
      </developer>
    </developers>,
  licenseFile := (ThisBuild / baseDirectory).value / "LICENSE",
  credentialsSetting,
  publishTo := sonatypePublishToBundle.value,
  Test / publishArtifact := false,
  // git发布
  releaseIgnoreUntrackedFiles := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := false"),
    publishSignedArtifacts,
    releaseStepCommand(s"rootNative/publishSigned"),
    releaseStepCommandAndRemaining("set ThisBuild / useSuperShell := true"),
    releaseStepCommandAndRemaining("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
) ++ Seq(packageBin, packageDoc, packageSrc).flatMap {
  // include LICENSE in all packaged artifacts
  inTask(_)(Seq((Compile / mappings) += licenseFile.value -> "LICENSE"))
}

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

lazy val noPublishSettings = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {},
  publishSigned := {},
  publishLocalSigned := {}
)

lazy val publishSignedArtifacts = ReleaseStep(
  action = st => {
    val extracted = st.extract
    val ref = extracted.get(thisProjectRef)
    extracted.runAggregated(ref / (Global / publishSigned), st)
  },
  check = st => {
    // getPublishTo fails if no publish repository is set up.
    val ex = st.extract
    val ref = ex.get(thisProjectRef)
    val (newState, value) = ex.runTask(ref / (Global / publishTo), st)
    Classpaths.getPublishTo(value)
    newState
  },
  enableCrossBuild = true
)

lazy val credentialsSetting = credentials ++= {
  val name = "Sonatype Nexus Repository Manager"
  val realm = "oss.sonatype.org"
  (
    sys.props.get("build.publish.user"),
    sys.props.get("build.publish.password"),
    sys.env.get("SONATYPE_USERNAME"),
    sys.env.get("SONATYPE_PASSWORD")
  ) match {
    case (Some(user), Some(pass), _, _) => Seq(Credentials(name, realm, user, pass))
    case (_, _, Some(user), Some(pass)) => Seq(Credentials(name, realm, user, pass))
    case _ =>
      val ivyFile = Path.userHome / ".ivy2" / ".credentials"
      val m2File = Path.userHome / ".m2" / "credentials"
      if (ivyFile.exists()) Seq(Credentials(ivyFile))
      else if (m2File.exists()) Seq(Credentials(m2File))
      else Nil
  }
}

lazy val licenseFile = settingKey[File]("The license file to include in packaged artifacts")

lazy val global = project.in(file(".")).settings(sharedSettings)
  .settings(noPublishSettings)
  .aggregate(core, server, benchmark)

lazy val core = project
  .settings(
    name := "duckling-core",
    sharedSettings,
    libraryDependencies ++= coreDependencies,
    publishArtifact := true
  )

lazy val server = project
  .settings(
    name := "duckling-server",
    sharedSettings,
    libraryDependencies ++= serverDependencies,
    // 打包
    Universal / javaOptions ++= Seq("-J-Xmx1g"),
    scriptClasspath := Seq("../conf", "*")
  )
  .settings(noPublishSettings)
  .dependsOn(core)
  .enablePlugins(JavaServerAppPackaging)

lazy val benchmark = project
  .settings(
    name := "duckling-benchmark",
    sharedSettings,
    libraryDependencies ++= benchmarkDependencies,
    outputStrategy := Some(StdoutOutput),
    fork := true,
    connectInput := true,
    logBuffered := false
  )
  .settings(noPublishSettings)
  .dependsOn(core)
