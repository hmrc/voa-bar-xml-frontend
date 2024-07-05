import com.typesafe.sbt.web.Import.*
import net.ground5hark.sbt.concat.Import.*
import com.typesafe.sbt.digest.Import.*
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "voa-bar-xml-frontend"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always // Resolves versions conflict

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / majorVersion := 1
ThisBuild / scalafmtFailOnErrors := true
ThisBuild / semanticdbEnabled := true


lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalacOptions += "-J-Xss16M",
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    RoutesKeys.routesImport ++= Seq("models._"),
    PlayKeys.playDefaultPort := 8448,
    libraryDependencies ++= Dependencies.appDependencies
  )
  .settings(
    Concat.groups := Seq(
      "javascripts/voabarxmlfrontend-app.js" -> group(Seq("javascripts/show-hide-content.js", "javascripts/voabarxmlfrontend.js"))
    ),
    Assets / pipelineStages := Seq(concat, digest)
  )

excludeDependencies ++= Seq(
  "org.scala-lang.modules" % "scala-parser-combinators_2.13"
)
