import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object Dependencies {

  private val bootstrapVersion = "5.20.0"
  private val playFrontendHmrcVersion = "3.5.0-play-28"
  private val playConditionalFormMappingVersion = "1.11.0-play-28"
  private val playLanguageVersion = "5.1.0-play-28"
  private val playPartialsVersion = "8.2.0-play-28"
  private val simpleReactiveMongo = "8.0.0-play-28"
  private val httpVerbsVersion = "13.12.0"
  private val httpCachingClientVersion = "9.5.0-play-28"
  private val logbackJsonLoggerVersion = "5.2.0"
  private val guiceUtilsVersion = "5.0.2"
  private val pdfBoxVersion = "2.0.24"
  private val uniformVersion = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion = "5.1.0"
  private val scalatestPlusScalacheckVersion = "3.2.11.0"
  private val scalatestVersion = "3.2.11"
  private val mockitoScalatestVersion = "1.17.0"
  private val flexmarkVersion = "0.62.2"

  val compileDependencies = Seq(
    ws,
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
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % scalatestPlusScalacheckVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % Test, // for scalatest 3.1+
    "com.typesafe.play" %% "play-test" % PlayVersion.current % Test,
    "com.typesafe.akka" %% "akka-testkit" % PlayVersion.akkaVersion % Test,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion % Test
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
