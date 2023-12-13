import sbt.Keys.*
import sbt.*
import scoverage.ScoverageKeys
import com.typesafe.sbt.web.Import.*
import net.ground5hark.sbt.concat.Import.*
import com.typesafe.sbt.digest.Import.*
import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.*
import DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

val appName = "voa-bar-xml-frontend"

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always // Resolves versions conflict

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(majorVersion := 1)
  .settings(RoutesKeys.routesImport ++= Seq("models._"))
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*identifiers;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;.*DataCacheConnector;.*AutobarsInterpreter;.*UniformController;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*FrontendAppConfig;.*Constraints;.*UniformMessageUtil;" +
      ".*Formatters;.*CheckYourAnswersHelper;.*FormHelpers;.*error_template.template;.*main_template.template;.*pageChrome.template;.*feedbackError.template;" +
      ".*cr05SubmissionConfirmation.template;.*task_list.template;.*cr05SubmissionSummary.template;",
    ScoverageKeys.coverageMinimumStmtTotal := 87,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
  .settings(scalaSettings)
  .settings(defaultSettings())
  .settings(
    scalaVersion := "2.13.12",
    DefaultBuildSettings.targetJvm := "jvm-11",
    scalacOptions += "-J-Xss8M",
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    PlayKeys.playDefaultPort := 8448,
    libraryDependencies ++= Dependencies.appDependencies,
    retrieveManaged := true
  )
  .settings(
    Concat.groups := Seq(
      "javascripts/voabarxmlfrontend-app.js" -> group(Seq("javascripts/show-hide-content.js", "javascripts/voabarxmlfrontend.js"))
    ),
    Assets / pipelineStages := Seq(concat, digest)
  )
