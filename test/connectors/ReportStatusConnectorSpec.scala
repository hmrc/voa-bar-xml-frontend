/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.OffsetDateTime

import base.SpecBase
import models.{Error, ReportStatus, Submitted}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import play.api.test.Helpers._
import play.mvc.Http.Status
import repositories.ReportStatusRepository

import scala.concurrent.ExecutionContext.Implicits.global

class ReportStatusConnectorSpec extends SpecBase with MockitoSugar {
  val baCode = "ba1221"
  val date = OffsetDateTime.now
  val submissionId = "1234-XX"
  val rs = ReportStatus(submissionId, date, userId = Some(baCode), status = Some(Submitted.value))
  val error = Error("Error", Seq())

  implicit val hc = mock[HeaderCarrier]
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val reportStatusRepositoryMock = mock[ReportStatusRepository]
  when(reportStatusRepositoryMock.getByUser(any[String])(any[ExecutionContext]))
    .thenReturn(Future(Right(Seq(rs))))
  when(reportStatusRepositoryMock.atomicSaveOrUpdate(any[ReportStatus], any[Boolean])(any[ExecutionContext]))
    .thenReturn(Future(Right(Unit)))
  when(reportStatusRepositoryMock.atomicSaveOrUpdate(any[String], any[String], any[Boolean])(any[ExecutionContext]))
    .thenReturn(Future(Right(Unit)))
  val reportStatusRepositoryFailMock = mock[ReportStatusRepository]
  when(reportStatusRepositoryFailMock.getByUser(any[String])(any[ExecutionContext]))
    .thenReturn(Future(Left(error)))
  when(reportStatusRepositoryFailMock.atomicSaveOrUpdate(any[ReportStatus], any[Boolean])(any[ExecutionContext]))
    .thenReturn(Future(Left(error)))
  when(reportStatusRepositoryFailMock.atomicSaveOrUpdate(any[String], any[String], any[Boolean])(any[ExecutionContext]))
    .thenReturn(Future(Left(error)))

  def getHttpMock(returnedStatus: Int, returnedJson: Option[JsValue]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, returnedJson))
    httpMock
  }

  "Report status connector spec" must {
    "given an username that was authorised by the voa - request the currently known report statuses from VOA-BAR" in {
      val connector = new DefaultReportStatusConnector(configuration, reportStatusRepositoryMock, environment)

      val result = await(connector.get("AUser"))

      result match {
        case Right(reportStatuses) => reportStatuses mustBe Seq(rs)
        case Left(_) => assert(false)
      }
    }
    
    "return a failure representing the error when send method fails" in {
      val httpClient = getHttpMock(Status.INTERNAL_SERVER_ERROR, None)
      val connector = new DefaultReportStatusConnector(configuration, reportStatusRepositoryFailMock, environment)
      
      val result = await(connector.get("AnOtherUser"))
      assert(result.isLeft)
    }

    "return a failure if the report status connector call throws an exception" in {
      val httpMock = mock[HttpClient]
      when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)

      val connector = new DefaultReportStatusConnector(configuration, reportStatusRepositoryFailMock, environment)
      val result = await(connector.get("AUSer"))
      assert(result.isLeft)
    }
  }
}
