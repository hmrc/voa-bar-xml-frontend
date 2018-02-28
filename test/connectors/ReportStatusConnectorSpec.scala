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

import base.SpecBase
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import play.api.test.Helpers.{status, _}

import scala.util.{Failure, Success, Try}

class ReportStatusConnectorSpec extends SpecBase with MockitoSugar {
  implicit val hc = mock[HeaderCarrier]
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]

  def getHttpMock(returnedStatus: Int, returnedJson: Option[JsValue]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, returnedJson))
    httpMock
  }

  val jsonStr =
    """[{"BAReportNumber": 987, "Errors": [], "SubmissionDate": "1-Mar-2018 09:15:26"},
      | {"BAReportNumber": 987, "Errors": [{"Code": 1001, "Detail": ["BAidentityNumber", "1656", "Bad formatting"]}, {"Code": 1002, "Detail": ["DateSent", "28-02-2018", "Invalid Format"]}], "SubmissionDate": "28-Feb-2018 14:28:36"},
      | {"BAReportNumber": 985, "Errors": [{"Code": 1000, "Detail": ["Town", "", "Empty Field Town"]}, {"Code": 1003, "Detail": ["DateSent", "27-01-2018", "Invalid Format"]}], "SubmissionDate": "27-Feb-2018 11:12:45"},
      | {"BAReportNumber": 766, "Errors": [], "SubmissionDate": "1-Jan-2018 17:08:53"}]""".stripMargin

  val json = Json.parse(jsonStr)

  "Report status connector spec" must {
    "given a username that was authorised by the voa - request the currently known report statuses from VOA-BAR" in {
      val httpClient = getHttpMock(200, Some(json))
      val connector = new ReportStatusConnector(httpClient, configuration, environment)

      val result = await(connector.request("AUser"))

      result match {
        case Success(jsValue) => jsValue mustBe json
        case Failure(e) => assert(false)
      }
    }
    
    "return a failure representing the error when send method fails" in {
      val httpClient = getHttpMock(500, None)
      val connector = new ReportStatusConnector(httpClient, configuration, environment)
      
      val result = await(connector.request("AnOtherUser"))
      result match {
        case Success(_) => assert(false)
        case Failure(e) => assert(true)
     }
    }

  }
}
