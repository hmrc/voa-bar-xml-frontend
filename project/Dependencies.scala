import sbt.*

object Dependencies {

  private val bootstrapVersion        = "10.7.0"
  private val hmrcMongoVersion        = "2.12.0"
  private val playFrontendHmrcVersion = "12.32.0"
  private val guiceUtilsVersion       = "6.0.0" // Use 6.0.0 because 7.0.0 is not compatible with play-guice:3.0.7
  private val pdfBoxVersion           = "3.0.7"
  private val uniformVersion          = "4.10.0"

  // Test dependencies
  private val scalatestPlusScalacheckVersion = "3.2.19.0"
  private val scalaTestPlusMockitoVersion    = "3.2.19.0"

  private val compileDependencies = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "net.codingwell"        %% "scala-guice"                % guiceUtilsVersion,
    "com.luketebbs.uniform" %% "interpreter-play28"         % uniformVersion cross CrossVersion.for3Use2_13,
    "org.apache.pdfbox"      % "pdfbox"                     % pdfBoxVersion
  )

  private val testDependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion               % Test,
    "org.scalatestplus" %% "scalacheck-1-18"        % scalatestPlusScalacheckVersion % Test,
    "org.scalatestplus" %% "mockito-5-12"           % scalaTestPlusMockitoVersion    % Test
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
