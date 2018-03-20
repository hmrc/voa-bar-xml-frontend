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

package controllers

import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions._
import identifiers.VOAAuthorisedId
import models.{NormalMode, ReportStatus}
import org.joda.time.DateTime
import org.mockito.Matchers.{any, anyString}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import views.html.reportStatus

import scala.concurrent.Future
import scala.util.Failure

class ReportStatusControllerSpec extends ControllerSpecBase with MockitoSugar {

  implicit val hc = mock[HeaderCarrier]
  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val username = "AUser"

  def getHttpMock(returnedStatus: Int, returnedJson: Option[JsValue]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, returnedJson))
    httpMock
  }

  val baCode = "ba1221"
  val submissionId1 = "1234-XX"
  val submissionId2 = "1235-XX"
  val submissionId3 = "1236-XX"

  val rs1 = ReportStatus(baCode, submissionId1, "SUBMITTED")
  val rs2 = ReportStatus(baCode, submissionId1, "VALIDATED")
  val rs3 = ReportStatus(baCode, submissionId1, "FORWARDED")

  Thread.sleep(1000)
  val rs11 = ReportStatus(baCode, submissionId2, "SUBMITTED")
  val rs22 = ReportStatus(baCode, submissionId2, "VALIDATED")
  val rs33 = ReportStatus(baCode, submissionId2, "FORWARDED")
  Thread.sleep(1000)
  val rs111 = ReportStatus(baCode, submissionId3, "SUBMITTED")
  val rs222 = ReportStatus(baCode, submissionId3, "VALIDATED")
  val rs333 = ReportStatus(baCode, submissionId3, "FORWARDED")

  val fakeMap = Map(submissionId1 -> List(rs1, rs2, rs3), submissionId2 -> List(rs11, rs22, rs33), submissionId3 -> List(rs111, rs222, rs333))
  val fakeMapAsJson = Json.toJson(fakeMap)
  val wrongJson = Json.toJson("""{"someID": "hhewfwe777"}""")

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)

  val sortedMap = fakeMap.map(x => (x._1, x._2.sortBy(_.created)))
  val sortedSubmissionIds = List(submissionId3, submissionId2, submissionId1)

  def fakeReportStatusConnector(json: JsValue) = new ReportStatusConnector(getHttpMock(200, Some(json)), configuration, environment)

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap, expectedJson: JsValue): ReportStatusController = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, fakeReportStatusConnector(expectedJson), dataRetrievalAction, new DataRequiredActionImpl)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, fakeReportStatusConnector(fakeMapAsJson), dataRetrievalAction, new DataRequiredActionImpl)
  }

  def viewAsString() = reportStatus(username, frontendAppConfig, sortedMap, sortedSubmissionIds)(fakeRequest, messages).toString

  "ReportStatus Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController(getEmptyCacheMap, fakeMapAsJson).onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "Given some Json representing a Report Status result, the verify response method creates a Right(Map[String, List[ReportStatus])" in {
      val result = loggedInController(getEmptyCacheMap, fakeMapAsJson).verifyResponse(fakeMapAsJson)

      result.isRight mustBe true
      result.right.get == fakeMap mustBe true
    }

    "Give some wrong Json, the verify response method returns a Left representing the exception to be thrown at Runtime" in {
      val result = loggedInController(getEmptyCacheMap, wrongJson).verifyResponse(wrongJson)

      result.isLeft mustBe true
      result mustBe Left("Unable to parse the response from the Report Status Connector")
    }

    "Throw a runtime exception when the received json value from the Report Status Connector cannot be parsed to a Map[String, List[ReportStatus]]" in {
      intercept[Exception] {
        val result = loggedInController(getEmptyCacheMap, wrongJson).onPageLoad()(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "Throw a runtime exception when  the Report Status returns an exception" in {
      val httpMock = mock[HttpClient]
      when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(Failure(new RuntimeException("Report status connector failed")))

      val connector = new ReportStatusConnector(httpMock, configuration, environment)
      val controller = new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, connector, getEmptyCacheMap, new DataRequiredActionImpl)

      intercept[Exception] {
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "The sortStatuses method should sort the List of Report Statuses by their creation time" in {
      val controller = loggedInController(getEmptyCacheMap, fakeMapAsJson)
      val result = controller.sortStatuses(fakeMap)

      result.head._2.head.created mustBe rs3.created
      result.last._2.head.created mustBe rs333.created
    }

    "The createDisplayOrder method should return a sorted List of submission Ids by their created time" in {
      val controller = loggedInController(getEmptyCacheMap, fakeMapAsJson)
      val sortedStatuses = controller.sortStatuses(fakeMap)
      val result = controller.createDisplayOrder(sortedStatuses)

      result.head mustBe submissionId3
      result.last mustBe submissionId1
    }
  }
}
