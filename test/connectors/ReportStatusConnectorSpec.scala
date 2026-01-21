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

import base.SpecBase
import models.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.*
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class ReportStatusConnectorSpec extends SpecBase with MockitoSugar:

  val userId       = "ba1221"
  val submissionId = "1234-XX"

  val rs    = ReportStatus(
    submissionId,
    baCode = Some(userId),
    status = Some(Submitted.value),
    createdAt = Instant.now.truncatedTo(ChronoUnit.SECONDS)
  )
  val error = Error("Error", Seq())

  val configuration = inject[Configuration]
  val environment   = inject[Environment]
  val exception     = new Exception
  val login         = Login("AUser", "anyPass")

  def servicesConfig = inject[ServicesConfig]

  val httpMock = mock[HttpClientV2]

  when(
    httpMock.get(any[URL])(using any[HeaderCarrier])
  ).thenReturn(RequestBuilderStub(Right(OK), Json.toJson(Seq(rs)).toString))

  when(
    httpMock.put(any[URL])(using any[HeaderCarrier])
  ).thenReturn(RequestBuilderStub(Right(OK), "{}"))

  val httpFailMock = mock[HttpClientV2]

  when(
    httpFailMock.get(any[URL])(using any[HeaderCarrier])
  ).thenReturn(RequestBuilderStub(Left(exception)))

  when(
    httpFailMock.put(any[URL])(using any[HeaderCarrier])
  ).thenReturn(RequestBuilderStub(Left(exception)))

  "Report status connector spec" must {
    "given an username that was authorised by the voa - request the currently known report statuses from VOA-BAR" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpMock, servicesConfig)
      val login       = Login("AUser", "anyPass")

      val result = await(connector.get(login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe Seq(rs)
        case Left(_)               => assert(false)
      }
    }

    "return a failure when the repository encounters an issue" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpFailMock, servicesConfig)

      val result = await(connector.get(login))

      assert(result.isLeft)
    }

    "returns a valid result when saving a new report" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpMock, servicesConfig)

      val result = await(connector.saveUserInfo(submissionId, login))

      assert(result.isRight)
    }

    "returns an error when saving a new report" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpFailMock, servicesConfig)

      val result = await(connector.saveUserInfo(submissionId, login))

      assert(result.isLeft)
    }

    "returns a valid result when saving a report" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpMock, servicesConfig)

      val result = await(connector.save(rs, login))

      assert(result.isRight)
    }

    "returns an error when saving a report" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpFailMock, servicesConfig)

      val result = await(connector.save(rs, login))

      assert(result.isLeft)
    }
    "given submission id get reportstatus" in {
      implicit val hc = HeaderCarrier()

      val httpClientV2Mock = mock[HttpClientV2]

      when(
        httpClientV2Mock.get(any[URL])(using any[HeaderCarrier])
      ).thenReturn(RequestBuilderStub(Right(OK), Json.toJson(rs).toString))

      val connector = new DefaultReportStatusConnector(httpClientV2Mock, servicesConfig)
      val login     = Login("AUser", "anyPass")

      val result = await(connector.getByReference(submissionId, login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe rs
        case Left(_)               => assert(false)
      }
    }

    "return a failure when the repository encounters an issue while retrieving submission" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpFailMock, servicesConfig)

      val result = await(connector.getByReference(submissionId, login))

      assert(result.isLeft)
    }

    "get all reportstatus" in {
      implicit val hc = HeaderCarrier()

      val connector = new DefaultReportStatusConnector(httpMock, servicesConfig)
      val login     = Login("AUser", "anyPass")

      val result = await(connector.getAll(login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe Seq(rs)
        case Left(_)               => assert(false)
      }
    }

    "return a failure when the repository encounters an issue while retrieving all submission" in {
      implicit val hc = HeaderCarrier()
      val connector   = new DefaultReportStatusConnector(httpFailMock, servicesConfig)

      val result = await(connector.getAll(login))

      assert(result.isLeft)
    }
  }
