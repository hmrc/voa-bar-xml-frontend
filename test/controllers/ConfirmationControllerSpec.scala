/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.actions.*
import identifiers.LoginId
import journey.UniformJourney.{Address, ContactDetails, Cr01Cr03Submission}
import journey.{AddProperty, Demolition, RemoveProperty}
import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import views.ViewSpecBase
import views.html.components.{confirmation_detail_panel, confirmation_status_panel}

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ConfirmationControllerSpec extends ControllerSpecBase with ViewSpecBase with MockitoSugar with Injecting {

  def ec                     = app.injector.instanceOf[ExecutionContext]
  def controllerComponents   = app.injector.instanceOf[MessagesControllerComponents]
  def reportConfirmationView = app.injector.instanceOf[views.html.govuk.confirmation]
  def confirmationView       = app.injector.instanceOf[views.html.confirmation]
  def errorTemplateView      = app.injector.instanceOf[views.html.error_template]

  def confirmationStatusPanel = inject[confirmation_status_panel]
  def confirmationDetailPanel = inject[confirmation_detail_panel]

  implicit def hc: HeaderCarrier = any[HeaderCarrier]

  val username                  = "AUser"
  val submissionId              = "SID372463"
  val login                     = Login("foo", "bar")
  val login2                    = Login(username, "bar")
  val reportStatus              = ReportStatus(submissionId, status = Some(Submitted.value))
  val reportStatusConnectorMock = mock[ReportStatusConnector]
  when(reportStatusConnectorMock.saveUserInfo(any[String], any[Login])).thenReturn(Future(Right(())))
  when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])).thenReturn(Future(Right(())))
  when(reportStatusConnectorMock.getByReference(any[String], any[Login])).thenReturn(Future(Right(reportStatus)))

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[Login](submissionId, LoginId.toString, login2)
    new ConfirmationController(
      frontendAppConfig,
      messagesApi,
      dataRetrievalAction,
      new DataRequiredActionImpl(ec),
      FakeDataCacheConnector,
      reportStatusConnectorMock,
      reportConfirmationView,
      confirmationView,
      confirmationStatusPanel,
      confirmationDetailPanel,
      errorTemplateView,
      controllerComponents
    )
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ConfirmationController(
      frontendAppConfig,
      messagesApi,
      dataRetrievalAction,
      new DataRequiredActionImpl(ec),
      FakeDataCacheConnector,
      reportStatusConnectorMock,
      reportConfirmationView,
      confirmationView,
      confirmationStatusPanel,
      confirmationDetailPanel,
      errorTemplateView,
      controllerComponents
    )
  }

  def cr01cr03ViewAsString(report: ReportStatus = reportStatus, cr01cr03Report: Option[Cr01Cr03Submission] = None) =
    reportConfirmationView(username, report, cr01cr03Report)(fakeRequest, messages).toString

  def viewAsString(report: ReportStatus = reportStatus, cr01cr03Report: Option[Cr01Cr03Submission] = None, submissionId: String = submissionId) =
    confirmationView(username, submissionId)(fakeRequest, messages).toString

  def refreshViewAsString() =
    confirmationView(username, submissionId, Some(reportStatus))(fakeRequest, messages).toString

  "Confirmation Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "return OK and the correct view for a GET - when status is verified" in {
      val verifiedSubmissionId = "VID372463"
      val verifiedReportStatus = ReportStatus(verifiedSubmissionId, status = Some(Verified.value))
      when(reportStatusConnectorMock.getByReference(eqTo(verifiedSubmissionId), any[Login]))
        .thenReturn(Future(Right(verifiedReportStatus)))

      val result = loggedInController().onPageLoad(verifiedSubmissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(verifiedReportStatus, submissionId = verifiedSubmissionId)
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad(submissionId)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return OK and the correct view for the refresh page" in {
      val result = loggedInController().onPageRefresh(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe refreshViewAsString()
    }

    "return OK and the correct view for the status check" in {
      val result = loggedInController().onStatusCheck(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result).as[JsObject].keys.contains("status") mustBe true
      contentAsJson(result).as[JsObject].keys.contains("statusPanel") mustBe true
    }

    "if while refreshing not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageRefresh(submissionId)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "if while checking the status not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onStatusCheck(submissionId)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "if CR03 report is present, it should render confirmation page with all details" in {
      val submissionId     = UUID.randomUUID().toString
      val cr03Report       = aCr03Report
      val cr03Json         = Json.obj(
        "type"       -> "Cr01Cr03Submission",
        "submission" -> Cr01Cr03Submission.format.writes(cr03Report)
      )
      val cr03ReportStatus = reportStatus.copy(report = Option(cr03Json), id = submissionId)

      when(reportStatusConnectorMock.getByReference(eqTo(submissionId), any[Login]))
        .thenReturn(Future(Right(cr03ReportStatus)))
      val result = loggedInController().onPageRefresh(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe cr01cr03ViewAsString(cr03ReportStatus, Some(cr03Report))
    }
  }

  def aCr03Report: Cr01Cr03Submission =
    Cr01Cr03Submission(
      AddProperty,
      None,
      None,
      "baRepro",
      "baRer",
      None,
      Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX"),
      ContactDetails("firstName", "lastName", Option("user@example.com"), Option("01122554442")),
      false,
      Option(Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX")),
      LocalDate.now(),
      true,
      Option("1122"),
      None,
      Some("comment")
    )

  "if CR01 report is present, it should render confirmation page with all details" in {
    val submissionId     = UUID.randomUUID().toString
    val cr01Report       = aCr01Report
    val cr01Json         = Json.obj(
      "type"       -> "Cr01Cr03Submission",
      "submission" -> Cr01Cr03Submission.format.writes(cr01Report)
    )
    val cr01ReportStatus = reportStatus.copy(report = Option(cr01Json), id = submissionId)

    when(reportStatusConnectorMock.getByReference(eqTo(submissionId), any[Login]))
      .thenReturn(Future(Right(cr01ReportStatus)))
    val result = loggedInController().onPageRefresh(submissionId)(fakeRequest)

    status(result) mustBe OK
    contentAsString(result) mustBe cr01cr03ViewAsString(cr01ReportStatus, Some(cr01Report))
  }

  def aCr01Report: Cr01Cr03Submission =
    Cr01Cr03Submission(
      RemoveProperty,
      Some(Demolition),
      None,
      "baRepro",
      "baRer",
      None,
      Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX"),
      ContactDetails("firstName", "lastName", Option("user@example.com"), Option("01122554442")),
      false,
      Option(Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX")),
      LocalDate.now(),
      true,
      Option("1122"),
      None,
      Some("comment")
    )

}
