import play.core.PlayVersion
import sbt._

object Dependencies {

  private val bootstrapVersion = "7.12.0"
  private val hmrcMongoVersion = "0.74.0"
  private val playFrontendHmrcVersion = "4.0.0-play-28"
  private val httpCachingClientVersion = "10.0.0-play-28"
  private val guiceUtilsVersion = "5.1.0"
  private val pdfBoxVersion = "2.0.27"
  private val uniformVersion = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion = "5.1.0"
  private val scalatestPlusScalacheckVersion = "3.2.14.0"
  private val scalatestVersion = "3.2.14"
  private val mockitoScalatestVersion = "1.17.12"
  private val flexMarkVersion = "0.64.0"

  private val compileDependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc" % playFrontendHmrcVersion,
    "uk.gov.hmrc"       %% "http-caching-client" % httpCachingClientVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion,
    "com.luketebbs.uniform" %% "interpreter-play28" % uniformVersion
  )

  private val testDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % scalatestPlusScalacheckVersion % Test,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % Test,
    "com.typesafe.akka" %% "akka-testkit" % PlayVersion.akkaVersion % Test,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all" % flexMarkVersion % Test // for scalatest 3.2.x
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
