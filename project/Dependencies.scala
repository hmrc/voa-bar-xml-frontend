import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val govukTemplateVersion = "5.72.0-play-28"
  private val playUiVersion = "9.7.0-play-28"
  private val bootstrapVersion = "5.19.0"
  private val playFrontendHmrcVersion = "2.0.0-play-28"
  private val playConditionalFormMappingVersion = "1.10.0-play-28"
  private val playLanguageVersion = "5.1.0-play-28"
  private val playPartialsVersion = "8.2.0-play-28"
  private val simpleReactiveMongo = "8.0.0-play-28"
  private val httpVerbsVersion = "13.10.0"
  private val httpCachingClientVersion = "9.5.0-play-28"
  private val logbackJsonLoggerVersion = "5.1.0"
  private val guiceUtilsVersion = "5.0.2"
  private val pdfBoxVersion = "2.0.24"
  private val uniformVersion = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion = "5.0.0"
  private val jsoupVersion = "1.14.3"
  private val mockitoScalatestVersion = "1.7.1"
  private val scalacheckVersion = "1.14.1"

  val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc"       %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc"       %% "play-ui" % playUiVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc" % playFrontendHmrcVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc"       %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc"       %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc"       %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "http-verbs-play-28" % httpVerbsVersion,
    "uk.gov.hmrc"       %% "simple-reactivemongo" % simpleReactiveMongo,
    "uk.gov.hmrc"       %% "logback-json-logger" % logbackJsonLoggerVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion,
    "com.luketebbs.uniform" %% "interpreter-play28" % uniformVersion
  )

  val testDependencies = Seq(
    //"org.scalatest" %% "scalatest" % "3.2.10" % Test, // TODO: Remove
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % Test,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % Test,
    "com.typesafe.akka" %% "akka-testkit" % PlayVersion.akkaVersion % Test,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion % Test,
    "org.jsoup" % "jsoup" % jsoupVersion % Test
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
