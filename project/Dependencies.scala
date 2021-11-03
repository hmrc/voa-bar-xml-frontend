import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val logbackJsonLoggerVersion = "5.1.0"
  private val govukTemplateVersion = "5.72.0-play-28"
  private val playUiVersion = "9.7.0-play-28"
  private val httpCachingClientVersion = "9.5.0-play-28"
  private val simpleReactiveMongo = "8.0.0-play-28"
  private val playConditionalFormMappingVersion = "1.10.0-play-28"
  private val playLanguageVersion = "5.1.0-play-28"
  private val bootstrapVersion = "5.16.0"
  private val guiceUtilsVersion = "5.0.2"
  private val pdfBoxVersion = "2.0.13"
  private val playPartialsVersion = "8.2.0-play-28"
  private val playFrontendHmrcVersion = "1.22.0-play-28"
  private val httpVerbsVersion = "13.10.0"
  private val uniformVersion = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion = "5.0.0"
  private val pegdownVersion = "1.6.0"
  private val jsoupVersion = "1.12.1"
  private val mockitoScalatestVersion = "1.7.1"
  private val scalacheckVersion = "1.14.1"
  private val akkaVersion = "2.6.14"

  val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo" % simpleReactiveMongo,
    "uk.gov.hmrc"       %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc"       %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc"       %% "play-ui" % playUiVersion,
    "uk.gov.hmrc"       %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc"       %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc" % playFrontendHmrcVersion,
    "uk.gov.hmrc"       %% "http-verbs-play-28" % httpVerbsVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion,
    "com.luketebbs.uniform" %% "core" % uniformVersion,
    "com.luketebbs.uniform" %% "interpreter-play28" % uniformVersion,
    "com.luketebbs.uniform" %% "interpreter-cli" % uniformVersion
  )

  def testDependencies(scope: String) = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % jsoupVersion % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % scope
  )

  val appDependencies = compileDependencies ++ testDependencies("test")
}
