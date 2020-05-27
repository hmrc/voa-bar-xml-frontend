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

package controllers

import java.time.{LocalDate, ZonedDateTime}
import java.util.UUID

import com.fasterxml.jackson.annotation.ObjectIdGenerators.UUIDGenerator
import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions._
import identifiers.LoginId
import journey.UniformJourney.{Address, ContactDetails, Cr03Submission}
import models.{Login, NormalMode, Pending, ReportStatus, Submitted}
import play.api.test.Helpers._
import views.html.confirmation
import org.mockito.scalatest.MockitoSugar
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{MessagesControllerComponents, Request}
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ConfirmationControllerSpec extends ControllerSpecBase with MockitoSugar {

  def ec = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  def reportConfirmationView = app.injector.instanceOf[views.html.govuk.confirmation]

  val username = "AUser"
  val submissionId = "SID372463"
  val login = Login("foo", "bar")
  val reportStatus = ReportStatus(submissionId, ZonedDateTime.now, status = Some(Submitted.value))
  val reportStatusConnectorMock = mock[ReportStatusConnector]
  when(reportStatusConnectorMock.saveUserInfo(any[String], any[Login])) thenReturn Future(Right(Unit))
  when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])) thenReturn Future(Right(Unit))
  when(reportStatusConnectorMock.getByReference(any[String], any[Login])) thenReturn Future(Right(reportStatus))

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[Login](submissionId, LoginId.toString, login)
    new ConfirmationController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl(ec), FakeDataCacheConnector, reportStatusConnectorMock, reportConfirmationView, controllerComponents)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ConfirmationController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl(ec), FakeDataCacheConnector, reportStatusConnectorMock,reportConfirmationView, controllerComponents)
  }

  def cr03ViewAsString(report: ReportStatus = reportStatus, cr03Report: Option[Cr03Submission] = None) =
    reportConfirmationView(report, cr03Report)(fakeRequest, messages).toString

  def viewAsString(report: ReportStatus = reportStatus, cr03Report: Option[Cr03Submission] = None) = confirmation(username, submissionId, frontendAppConfig)(fakeRequest, messages).toString
  def refreshViewAsString() =
    confirmation(username, submissionId, frontendAppConfig, Some(reportStatus))(fakeRequest, messages).toString

  "Confirmation Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
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

    "if while refreshing not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageRefresh(submissionId)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "if CR03 report is present, it should render confirmation page with all details" in {
      val submissionId = UUID.randomUUID().toString
      val cr03Report = aCr03Report
      val cr03Json = Json.obj(
        "type" -> "Cr03Submission",
        "submission" -> Cr03Submission.format.writes(cr03Report)
      )
      val cr03ReportStatus = reportStatus.copy(report = Option(cr03Json), id = submissionId)

      when(reportStatusConnectorMock.getByReference(eqTo(submissionId), any[Login]))
        .thenReturn(Future(Right(cr03ReportStatus)))
      val result = loggedInController().onPageRefresh(submissionId)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe cr03ViewAsString(cr03ReportStatus, Some(cr03Report))
    }
  }

  def aCr03Report: Cr03Submission = {
    Cr03Submission("baRepro", "baRer", None,
      Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX"),
      ContactDetails("firstName", "lastName", Option("user@example.com"), Option("01122554442")),
      false,
      Option(Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX")),
      LocalDate.now(), true, Option("1122"), None, Some("comment")
    )
  }

}
