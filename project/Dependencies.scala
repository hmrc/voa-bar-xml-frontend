import sbt.*

object Dependencies {

  private val bootstrapVersion        = "10.7.0"
  private val playFrontendHmrcVersion = "13.3.0"
  private val voServiceVersion        = "0.4.0"
  private val hmrcMongoVersion        = "2.12.0"
  private val guiceUtilsVersion       = "6.0.0" // Use 6.0.0 because 7.0.0 is not compatible with play-guice:3.0.7
  private val jqueryVersion           = "2.2.4" // jQuery 2.2.4 includes .ajax() function
  private val pdfBoxVersion           = "3.0.7"
  private val uniformVersion          = "4.10.0"

  // Test dependencies
  private val voTestVersion = "0.3.0"

  private val compileDependencies = Seq(
    "uk.gov.hmrc"           %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"           %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc"           %% "vo-frontend-service"        % voServiceVersion,
    "uk.gov.hmrc.mongo"     %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "net.codingwell"        %% "scala-guice"                % guiceUtilsVersion,
    "com.luketebbs.uniform" %% "interpreter-play28"         % uniformVersion cross CrossVersion.for3Use2_13,
    "org.webjars"            % "jquery"                     % jqueryVersion,
    "org.apache.pdfbox"      % "pdfbox"                     % pdfBoxVersion
  )

  private val testDependencies = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "uk.gov.hmrc" %% "vo-unit-test"           % voTestVersion    % Test
  )

  val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

}
