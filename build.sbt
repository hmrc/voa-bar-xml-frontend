import org.irundaia.sass.Minified
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings.targetJvm
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin

val appName = "voa-bar-xml-frontend"

ThisBuild / scalaVersion := "3.8.3"
ThisBuild / majorVersion := 1
ThisBuild / semanticdbEnabled := true

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    maintainer := "voa.service.optimisation@digital.hmrc.gov.uk",
    targetJvm := "jvm-21",
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:msg=Flag .* set repeatedly:s",
    scalacOptions += "-Wconf:msg=Implicit parameters should be provided with a \\`using\\` clause&src=views/.*:s",
    javaOptions += "-XX:+EnableDynamicAgentLoading",
    RoutesKeys.routesImport ++= Seq("models._"),
    PlayKeys.playDefaultPort := 8448,
    libraryDependencies ++= Dependencies.appDependencies
  )
  .settings(
    SassKeys.cssStyle := Minified,
    SassKeys.generateSourceMaps := false,
    Concat.groups := Seq(
      "javascripts/app.js" -> group(Seq("javascripts/show-hide-content.js", "javascripts/main.js"))
    ),
    digest / includeFilter := GlobFilter("*.js") || GlobFilter("*.min.css"),
    Assets / pipelineStages := Seq(concat, digest)
  )

excludeDependencies ++= Seq( // Exclude dependencies added by com.luketebbs.uniform:interpreter-play28_2.13
  "org.scala-lang.modules" % "scala-parser-combinators_2.13"
)

addCommandAlias("scalastyle", "scalafmtAll;scalafmtSbt;scalafixAll")
