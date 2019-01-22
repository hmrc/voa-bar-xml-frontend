import sbt._


object Dependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val playHealthVersion = "3.9.0-play-25"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val govukTemplateVersion = "5.26.0-play-25"
  private val playUiVersion = "7.27.0-play-25"
  private val hmrcTestVersion = "3.3.0"
  private val scalaTestVersion = "3.0.4"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val pegdownVersion = "1.6.0"
  private val mockitoAllVersion = "1.10.19"
  private val httpCachingClientVersion = "7.1.0"
  private val playReactivemongoVersion = "6.2.0"
  private val playConditionalFormMappingVersion = "0.2.0"
  private val playLanguageVersion = "3.4.0"
  private val bootstrapVersion = "4.3.0"
  private val scalacheckVersion = "1.13.4"
  private val catsVersion = "1.1.0"
  private val guiceUtilsVersion = "4.1.0"
  private val pdfBoxVersion = "2.0.1"
  private val playPartialsVersion = "6.3.0"


  val compileDependencies = Seq(
    ws,
    "uk.gov.hmrc"       %% "play-reactivemongo" % playReactivemongoVersion,
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
        "org.jsoup" % "jsoup" % "1.10.3" % Test,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % Test,
        "org.mockito" % "mockito-all" % mockitoAllVersion % Test,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % Test
      )

  val appDependencies = compileDependencies ++ testDependencies
}
