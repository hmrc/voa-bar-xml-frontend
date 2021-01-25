/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.ZonedDateTime
import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions._
import identifiers.LoginId
import models._
import org.joda.time.DateTime
import org.mockito.scalatest.MockitoSugar
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import services.ReceiptService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.{TableFormatter, ViewSpecBase}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class ReportStatusControllerSpec extends ControllerSpecBase with ViewSpecBase with MockitoSugar {

  //implicit val hc = mock[HeaderCarrier]

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val username = "AUser"
  val login = Login("foo", "bar")
  val login2 = Login(username, "bar")

  val baCode = "ba1221"
  val submissionId1 = "1234-XX"
  val submissionId2 = "1235-XX"
  val submissionId3 = "1236-XX"
  val date = () => ZonedDateTime.now

  val rs1 = ReportStatus(submissionId1, date(), baCode = Some(baCode), status = Some(Submitted.value))
  val rs2 = ReportStatus(submissionId1, date(), baCode = Some(baCode), status = Some(Verified.value))
  val rs3 = ReportStatus(submissionId1, date(), baCode = Some(baCode), status = Some(Done.value))

  val fakeReports = Seq(rs1, rs2, rs3)
  val fakeMapAsJson = Json.toJson(fakeReports)
  val wrongJson = Json.toJson("""{"someID": "hhewfwe777"}""")
  def servicesConfig = injector.instanceOf[ServicesConfig]
  val fakeTableFormatter = new TableFormatter(servicesConfig)

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)

  val sortedReports = fakeReports.sortWith{ case (r, r2) => r.created.compareTo(r2.created) >= 0 }
  val sortedSubmissionIds = List(submissionId3, submissionId2, submissionId1)

  val receiptServiceMock = mock[ReceiptService]
  when(receiptServiceMock.producePDF(any[ReportStatus])) thenReturn(Success(Array[Byte](1, 2, 3, 4)))

  def ec = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def fakeReportStatusConnector() = {
    val reportStatusConnectorMock = mock[ReportStatusConnector](withSettings.lenient())
    when(reportStatusConnectorMock.get(any[Login], any[Option[String]])(any[HeaderCarrier])).thenReturn(Future(Right(fakeReports)))
    reportStatusConnectorMock
  }

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap, expectedJson: JsValue): ReportStatusController = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[Login]("", LoginId.toString, login2)
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, fakeReportStatusConnector(),
      dataRetrievalAction, new DataRequiredActionImpl(ec), receiptServiceMock, createReportStatusView(), createErrorTemplateView(), controllerComponents, fakeTableFormatter)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, fakeReportStatusConnector(),
      dataRetrievalAction, new DataRequiredActionImpl(ec), receiptServiceMock, createReportStatusView(), createErrorTemplateView(), controllerComponents, fakeTableFormatter)
  }

  def viewAsString() = createReportStatusView()(username, fakeReports, None, fakeTableFormatter)(fakeRequest, messages).toString

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
      result.right.get == fakeReports mustBe true
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
      val reportStatusConnectorMock = mock[ReportStatusConnector](withSettings.lenient())
      when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])(any[HeaderCarrier])) thenReturn Future(Right(Unit))

      val controller =
        new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, reportStatusConnectorMock,
          getEmptyCacheMap, new DataRequiredActionImpl(ec), receiptServiceMock, createReportStatusView(),
          createErrorTemplateView(), controllerComponents, fakeTableFormatter)

      intercept[Exception] {
        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "return OK when trying to download a report status" in {
      val reportStatusConnectorMock = mock[ReportStatusConnector]
      when(reportStatusConnectorMock.getByReference(any[String], any[Login])(any[HeaderCarrier])) thenReturn Future(Right(rs1))
      FakeDataCacheConnector.resetCaptures()
      FakeDataCacheConnector.save[Login]("", LoginId.toString, login)

      val controller =
        new ReportStatusController(frontendAppConfig, messagesApi,
          FakeDataCacheConnector, reportStatusConnectorMock, getEmptyCacheMap,new DataRequiredActionImpl(ec),
          receiptServiceMock, createReportStatusView(), createErrorTemplateView(),controllerComponents, fakeTableFormatter)

      val result = controller.onReceiptDownload(submissionId1)(fakeRequest)

      status(result) mustBe OK
    }

    "return OK when trying to download all the report statuses" in {
      val reportStatusConnectorMock = mock[ReportStatusConnector]
      when(reportStatusConnectorMock.getAll(any[Login])(any[HeaderCarrier])) thenReturn (Future(Right(Seq(rs1))))
      FakeDataCacheConnector.resetCaptures()
      FakeDataCacheConnector.save[Login]("", LoginId.toString, login)

      val controller =
        new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, reportStatusConnectorMock,
          getEmptyCacheMap, new DataRequiredActionImpl(ec), receiptServiceMock, createReportStatusView(), createErrorTemplateView(), controllerComponents, fakeTableFormatter)

      val result = controller.onAllReceiptsDownload()(fakeRequest)

      status(result) mustBe OK
    }

    "return 500 when trying to download all the report statuses" in {
      val reportStatusConnectorMock = mock[ReportStatusConnector]
      when(reportStatusConnectorMock.getAll(any[Login])(any[HeaderCarrier])) thenReturn Future(Left(Error("error", Seq())))
      FakeDataCacheConnector.resetCaptures()
      FakeDataCacheConnector.save[Login]("", LoginId.toString, login)

      val controller =
        new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, reportStatusConnectorMock,
          getEmptyCacheMap, new DataRequiredActionImpl(ec), receiptServiceMock, createReportStatusView(),
          createErrorTemplateView(), controllerComponents, fakeTableFormatter)

      val result = controller.onAllReceiptsDownload()(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
