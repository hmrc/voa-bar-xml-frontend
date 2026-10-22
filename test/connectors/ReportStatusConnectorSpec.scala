/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import models.*
import org.scalatest.AppendedClues.convertToClueful
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.vo.unit.test.BaseAppSpec

import java.time.Instant
import java.time.temporal.ChronoUnit

class ReportStatusConnectorSpec extends BaseAppSpec:

  private val userId       = "ba1221"
  private val submissionId = "1234-XX"

  private val rs = ReportStatus(
    submissionId,
    baCode = Some(userId),
    status = Some(Submitted.value),
    createdAt = Instant.now.truncatedTo(ChronoUnit.SECONDS)
  )

  private val exception      = Exception("failure")
  private val login          = Login("AUser", "anyPass")
  private val servicesConfig = inject[ServicesConfig]

  "Report status connector spec" should {
    "given an username that was authorised by the VO - request the currently known report statuses from voa-bar" in {
      val connector = DefaultReportStatusConnector(httpClientMock(responseBody = Json.toJson(Seq(rs))), servicesConfig)
      val login     = Login("AUser", "anyPass")

      val result = connector.get(login).futureValue
      result match
        case Right(reportStatuses) => reportStatuses shouldBe Seq(rs)
        case Left(error)           => fail(s"Get report statuses failed: $error")
    }

    "return a failure when the repository encounters an issue" in {
      val connector = DefaultReportStatusConnector(httpClientFailedMock(returnFailure = exception), servicesConfig)

      val result = connector.get(login).futureValue
      result.isLeft shouldBe true withClue "Failure is expected"
    }

    "returns a valid result when saving a new report" in {
      val connector = DefaultReportStatusConnector(httpClientMock(PUT, responseBody = "{}"), servicesConfig)

      val result = connector.saveUserInfo(submissionId, login).futureValue
      result.isRight shouldBe true withClue "Valid result is expected"
    }

    "returns an error when saving a new report" in {
      val connector = DefaultReportStatusConnector(httpClientFailedMock(PUT, returnFailure = exception), servicesConfig)

      val result = connector.saveUserInfo(submissionId, login).futureValue
      result.isLeft shouldBe true withClue "Error is expected"
    }

    "returns a valid result when saving a report" in {
      val connector = DefaultReportStatusConnector(httpClientMock(PUT, responseBody = "{}"), servicesConfig)

      val result = connector.save(rs, login).futureValue
      result.isRight shouldBe true withClue "Valid result is expected"
    }

    "returns an error when saving a report" in {
      val connector = DefaultReportStatusConnector(httpClientFailedMock(PUT, returnFailure = exception), servicesConfig)

      val result = connector.save(rs, login).futureValue
      result.isLeft shouldBe true withClue "Failure is expected"
    }

    "given submission id get report status" in {
      val connector = DefaultReportStatusConnector(httpClientMock(responseBody = Json.toJson(rs)), servicesConfig)
      val login     = Login("AUser", "anyPass")

      val result = connector.getByReference(submissionId, login).futureValue
      result match
        case Right(reportStatuses) => reportStatuses shouldBe rs
        case Left(error)           => fail(s"Get report statuses failed: $error")
    }

    "return a failure when the repository encounters an issue while retrieving submission" in {
      val connector = DefaultReportStatusConnector(httpClientFailedMock(returnFailure = exception), servicesConfig)

      val result = connector.getByReference(submissionId, login).futureValue
      result.isLeft shouldBe true withClue "Failure is expected"
    }

    "get all report statuses" in {
      val connector = DefaultReportStatusConnector(httpClientMock(responseBody = Json.toJson(Seq(rs))), servicesConfig)
      val login     = Login("AUser", "anyPass")

      val result = connector.getAll(login).futureValue
      result match
        case Right(reportStatuses) => reportStatuses shouldBe Seq(rs)
        case Left(error)           => fail(s"Get report statuses failed: $error")
    }

    "return a failure when the repository encounters an issue while retrieving all submission" in {
      val connector = DefaultReportStatusConnector(httpClientFailedMock(returnFailure = exception), servicesConfig)

      val result = connector.getAll(login).futureValue
      result.isLeft shouldBe true withClue "Failure is expected"
    }
  }
