import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "3.14.0-play-25"
  private val logbackJsonLoggerVersion = "4.6.0"
  private val govukTemplateVersion = "5.46.0-play-25"
  private val playUiVersion = "8.6.0-play-25"
  private val hmrcTestVersion = "3.4.0-play-25"
  private val scalaTestVersion = "3.0.4"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "9.0.0-play-25"
  private val simpleReactiveMongo = "7.22.0-play-25"
  private val playConditionalFormMappingVersion = "1.2.0-play-25"
  private val playLanguageVersion = "4.2.0-play-25"
  private val bootstrapVersion = "5.1.0"
  private val scalacheckVersion = "1.13.4"
  private val catsVersion = "1.6.1"
  private val guiceUtilsVersion = "4.2.2"
  private val pdfBoxVersion = "2.0.13"
  private val playPartialsVersion = "6.9.0-play-25"


  val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo" % simpleReactiveMongo,
    "uk.gov.hmrc"       %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc"       %% "govuk-template" % govukTemplateVersion,
    "uk.gov.hmrc"       %% "play-health" % playHealthVersion,
    "uk.gov.hmrc"       %% "play-ui" % playUiVersion,
    "uk.gov.hmrc"       %% "play-partials" % playPartialsVersion excludeAll(
      ExclusionRule("uk.gov.hmrc",  "http-verbs-play-25_2.11")   // This library is deprecated
    ),
    "uk.gov.hmrc"       %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping" % playConditionalFormMappingVersion,
    "uk.gov.hmrc"       %% "bootstrap-play-25" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-language" % playLanguageVersion,
    "org.typelevel"     %% "cats-core" % catsVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion
  )

  lazy val testDependencies = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % Test,
        "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test,
        "org.pegdown" % "pegdown" % pegdownVersion % Test,
        "org.jsoup" % "jsoup" % "1.11.3" % Test,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % Test,
        "org.mockito" % "mockito-all" % mockitoAllVersion % Test,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test
      )

  val appDependencies = compileDependencies ++ testDependencies
}
