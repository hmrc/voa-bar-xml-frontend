import play.core.PlayVersion
import sbt.*

object Dependencies {

  private val bootstrapVersion        = "10.5.0"
  private val hmrcMongoVersion        = "2.11.0"
  private val playFrontendHmrcVersion = "12.27.0"
  private val guiceUtilsVersion       = "6.0.0" // Use 6.0.0 because 7.0.0 is not compatible with play-guice:3.0.7
  private val pdfBoxVersion           = "3.0.6"
  private val uniformVersion          = "4.10.0"

  // Test dependencies
  private val scalaTestPlusPlayVersion       = "7.0.2"
  private val scalatestVersion               = "3.2.19"
  private val scalatestPlusScalacheckVersion = "3.2.19.0"
  private val scalaTestPlusMockitoVersion    = "3.2.19.0"
  private val flexMarkVersion                = "0.64.8"

  private val compileDependencies = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "net.codingwell"        %% "scala-guice"                % guiceUtilsVersion,
    "com.luketebbs.uniform" %% "interpreter-play28"         % uniformVersion cross CrossVersion.for3Use2_13,
    "org.apache.pdfbox"      % "pdfbox"                     % pdfBoxVersion
  )

  private val testDependencies = Seq(
    "org.playframework"      %% "play-test"          % PlayVersion.current            % Test,
    "org.apache.pekko"       %% "pekko-testkit"      % PlayVersion.pekkoVersion       % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion       % Test,
    "org.scalatest"          %% "scalatest"          % scalatestVersion               % Test,
    "org.scalatestplus"      %% "scalacheck-1-18"    % scalatestPlusScalacheckVersion % Test,
    "org.scalatestplus"      %% "mockito-5-12"       % scalaTestPlusMockitoVersion    % Test,
    "com.vladsch.flexmark"    % "flexmark-all"       % flexMarkVersion                % Test // for scalatest 3.2.x
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
