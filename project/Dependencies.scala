import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "3.14.0-play-25"
  private val logbackJsonLoggerVersion = "4.6.0"
  private val govukTemplateVersion = "5.46.0-play-26"
  private val playUiVersion = "8.6.0-play-26"
  private val hmrcTestVersion = "3.4.0-play-26"
  private val scalaTestVersion = "3.0.4"
  private val scalaTestPlusPlayVersion = "3.1.3"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "9.0.0-play-26"
  private val simpleReactiveMongo = "7.23.0-play-26"
  private val playConditionalFormMappingVersion = "1.2.0-play-26"
  private val playLanguageVersion = "4.2.0-play-26"
  private val bootstrapVersion = "1.3.0"
  private val scalacheckVersion = "1.14.1"
  private val catsVersion = "1.6.1"
  private val guiceUtilsVersion = "4.2.2"
  private val pdfBoxVersion = "2.0.13"
  private val playPartialsVersion = "6.9.0-play-26"
  private val playFrontendGovUkVersion = "0.40.0-play-26"

  private val akkaVersion     = "2.5.23"
  private val akkaHttpVersion = "10.0.15"


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
    "org.typelevel"     %% "cats-core" % catsVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion
  )

  val dependencyOverrides = Seq(
    "com.typesafe.akka" %% "akka-stream"    % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-protobuf"  % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-actor"     % akkaVersion     force(),
    "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion force()
  )

  def testDependencies(scope: String) = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "org.jsoup" % "jsoup" % "1.12.1" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.mockito" %% "mockito-scala-scalatest" % "1.7.1" % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope
  )

  val appDependencies = compileDependencies ++ testDependencies("test") ++ testDependencies("it")
}
