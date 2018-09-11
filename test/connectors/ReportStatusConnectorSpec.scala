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
import models.{Error, ReportStatus}
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import play.api.test.Helpers.{status, _}
import play.mvc.Http.Status
import repositories.ReportStatusRepository

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class ReportStatusConnectorSpec extends SpecBase with MockitoSugar {
  implicit val hc = mock[HeaderCarrier]
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val date = OffsetDateTime.now
  val reportStatusRepositoryMock = mock[ReportStatusRepository]

  def getHttpMock(returnedStatus: Int, returnedJson: Option[JsValue]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, returnedJson))
    httpMock
  }

  val baCode = "ba1221"
  val submissionId = "1234-XX"
  val rs = ReportStatus(submissionId, date, userId = Some(baCode), status = Some("SUBMITTED"))
  val fakeMap = Map(submissionId -> List(rs))
  val fakeMapAsJson = Json.toJson(fakeMap)

  "Report status connector spec" must {
    "given an username that was authorised by the voa - request the currently known report statuses from VOA-BAR" in {
      val httpClient = getHttpMock(Status.OK, Some(fakeMapAsJson))
      val connector = new DefaultReportStatusConnector(httpClient, configuration, reportStatusRepositoryMock, environment)

      val result = await(connector.request("AUser"))

      result match {
        case Success(jsValue) => jsValue mustBe fakeMapAsJson
        case Failure(e) => assert(false)
      }
    }
    
    "return a failure representing the error when send method fails" in {
      val httpClient = getHttpMock(Status.INTERNAL_SERVER_ERROR, None)
      val connector = new DefaultReportStatusConnector(httpClient, configuration, reportStatusRepositoryMock, environment)
      
      val result = await(connector.request("AnOtherUser"))
      assert(result.isFailure)
    }

    "return a failure if the report status connector call throws an exception" in {
      val httpMock = mock[HttpClient]
      when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)

      val connector = new DefaultReportStatusConnector(httpMock, configuration, reportStatusRepositoryMock, environment)
      val result = await(connector.request("AUSer"))
      assert(result.isFailure)
    }
  }
}
