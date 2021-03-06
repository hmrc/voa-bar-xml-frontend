import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "3.16.0-play-26"
  private val logbackJsonLoggerVersion = "5.1.0"
  private val govukTemplateVersion = "5.58.0-play-26"
  private val playUiVersion = "8.14.0-play-26"
  private val scalaTestPlusPlayVersion = "3.1.3"
  private val pegdownVersion = "1.6.0"
  private val httpCachingClientVersion = "9.2.0-play-26"
  private val simpleReactiveMongo = "7.31.0-play-26"
  private val playConditionalFormMappingVersion = "1.6.0-play-26"
  private val playLanguageVersion = "4.10.0-play-26"
  private val bootstrapVersion = "2.3.0"
  private val scalacheckVersion = "1.14.1"
  private val guiceUtilsVersion = "4.2.2"
  private val pdfBoxVersion = "2.0.13"
  private val playPartialsVersion = "7.1.0-play-26"
  private val playFrontendGovUkVersion = "0.63.0-play-26"
  private val playFrontendHmrcVersion  = "0.48.0-play-26"
  private val akkaVersion     = "2.5.26"

  val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo" % simpleReactiveMongo,
    "uk.gov.hmrc"       %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc"       %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc"       %% "play-health" % playHealthVersion,
    "uk.gov.hmrc"       %% "play-ui" % playUiVersion,
    "uk.gov.hmrc"       %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc"       %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc"       %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-language" % playLanguageVersion,
    "uk.gov.hmrc"       %% "play-frontend-govuk" % playFrontendGovUkVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc" % playFrontendHmrcVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion,
    "com.luketebbs.uniform" %% "core" % "4.10.0",
    "com.luketebbs.uniform" %% "interpreter-play26" % "4.10.0",
    "com.luketebbs.uniform" %% "interpreter-cli" % "4.10.0"
  )

  def testDependencies(scope: String) = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % "1.12.1" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" %% "mockito-scala-scalatest" % "1.7.1" % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % scope
  )

  val appDependencies = compileDependencies ++ testDependencies("test")
}
