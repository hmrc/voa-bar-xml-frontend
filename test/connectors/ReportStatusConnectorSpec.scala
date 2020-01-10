/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.ZonedDateTime

import base.SpecBase
import models._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Writes
import play.api.{Configuration, Environment}

import scala.concurrent.{ExecutionContext, Future}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class ReportStatusConnectorSpec extends SpecBase with MockitoSugar {
  val userId = "ba1221"
  val date = ZonedDateTime.now
  val submissionId = "1234-XX"
  val rs = ReportStatus(submissionId, date, baCode = Some(userId), status = Some(Submitted.value))
  val error = Error("Error", Seq())

  implicit val hc = HeaderCarrier()
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val httpResponse = mock[HttpResponse]
  val exception = new Exception
  val login = Login("AUser", "anyPass")
  val httpMock = mock[HttpClient]
  when(httpMock.GET[Seq[ReportStatus]](any[String])
    (any[HttpReads[Seq[ReportStatus]]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future(Seq(rs)))
  when(httpMock.PUT[ReportStatus, HttpResponse](any[String], any[ReportStatus], any[Seq[(String,String)]])
    (any[Writes[ReportStatus]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future(httpResponse))
  val httpFailMock = mock[HttpClient]
  when(httpFailMock.GET[Seq[ReportStatus]](any[String])
    (any[HttpReads[Seq[ReportStatus]]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(exception))
  when(httpFailMock.PUT[ReportStatus, HttpResponse](any[String], any[ReportStatus], any[Seq[(String,String)]])
    (any[Writes[ReportStatus]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.failed(exception))

  "Report status connector spec" must {
    "given an username that was authorised by the voa - request the currently known report statuses from VOA-BAR" in {
      val connector = new DefaultReportStatusConnector(configuration, httpMock, environment)
      val login = Login("AUser", "anyPass")

      val result = await(connector.get(login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe Seq(rs)
        case Left(_) => assert(false)
      }
    }
    
    "return a failure when the repository encounters an issue" in {
      val connector = new DefaultReportStatusConnector(configuration, httpFailMock, environment)
      
      val result = await(connector.get(login))

      assert(result.isLeft)
    }

    "returns a valid result when saving a new report" in {
      val connector = new DefaultReportStatusConnector(configuration, httpMock, environment)

      val result = await(connector.saveUserInfo(submissionId, login))

      assert(result.isRight)
    }

    "returns an error when saving a new report" in {
      val connector = new DefaultReportStatusConnector(configuration, httpFailMock, environment)

      val result = await(connector.saveUserInfo(submissionId, login))

      assert(result.isLeft)
    }

    "returns a valid result when saving a report" in {
      val connector = new DefaultReportStatusConnector(configuration, httpMock, environment)

      val result = await(connector.save(rs, login))

      assert(result.isRight)
    }

    "returns an error when saving a report" in {
      val connector = new DefaultReportStatusConnector(configuration, httpFailMock, environment)

      val result = await(connector.save(rs, login))

      assert(result.isLeft)
    }
    "given submission id get reportstatus" in {
      val http = mock[HttpClient]
      when(http.GET[ReportStatus](any[String])
        (any[HttpReads[ReportStatus]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future(rs))
      val connector = new DefaultReportStatusConnector(configuration, http, environment)
      val login = Login("AUser", "anyPass")

      val result = await(connector.getByReference(submissionId, login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe rs
        case Left(_) => assert(false)
      }
    }

    "return a failure when the repository encounters an issue while retrieving submission" in {
      val connector = new DefaultReportStatusConnector(configuration, httpFailMock, environment)

      val result = await(connector.getByReference(submissionId, login))

      assert(result.isLeft)
    }

    "get all reportstatus" in {
      val http = mock[HttpClient]
      when(http.GET[Seq[ReportStatus]](any[String])
        (any[HttpReads[Seq[ReportStatus]]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future(Seq(rs)))
      val connector = new DefaultReportStatusConnector(configuration, http, environment)
      val login = Login("AUser", "anyPass")

      val result = await(connector.getAll(login))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe Seq(rs)
        case Left(_) => assert(false)
      }
    }

    "return a failure when the repository encounters an issue while retrieving all submission" in {
      val connector = new DefaultReportStatusConnector(configuration, httpFailMock, environment)

      val result = await(connector.getAll(login))

      assert(result.isLeft)
    }
  }
}
