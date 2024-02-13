import play.core.PlayVersion
import sbt.*

object Dependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"
  private val playFrontendHmrcVersion = "8.5.0"
  private val guiceUtilsVersion = "6.0.0"
  private val pdfBoxVersion = "2.0.30"
  private val uniformVersion = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion = "7.0.0"
  private val scalatestPlusScalacheckVersion = "3.2.17.0"
  private val scalatestVersion = "3.2.17"
  private val mockitoScalatestVersion = "1.17.30"
  private val flexMarkVersion = "0.64.8"

  private val compileDependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "net.codingwell"    %% "scala-guice" % guiceUtilsVersion,
    "org.apache.pdfbox" %  "pdfbox" % pdfBoxVersion,
    "com.luketebbs.uniform" %% "interpreter-play28" % uniformVersion
  )

  private val testDependencies = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % Test,
    "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    "org.scalatestplus" %% "scalacheck-1-17" % scalatestPlusScalacheckVersion % Test,
    "org.playframework" %% "play-test" % PlayVersion.current % Test,
    "org.apache.pekko"  %% "pekko-testkit" % PlayVersion.pekkoVersion % Test,
    "org.mockito" %% "mockito-scala-scalatest" % mockitoScalatestVersion % Test,
    "com.vladsch.flexmark" % "flexmark-all" % flexMarkVersion % Test // for scalatest 3.2.x
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
