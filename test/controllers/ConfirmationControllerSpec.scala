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

import java.time.OffsetDateTime

import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions._
import identifiers.LoginId
import models.{Login, NormalMode, ReportStatus}
import play.api.test.Helpers._
import views.html.confirmation
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ConfirmationControllerSpec extends ControllerSpecBase with MockitoSugar {

  val username = "AUser"
  val submissionId = "SID372463"
  val login = Login("foo", "bar")
  val reportStatus = ReportStatus(submissionId, OffsetDateTime.now)
  val reportStatusConnectorMock = mock[ReportStatusConnector]
  when(reportStatusConnectorMock.saveUserInfo(any[String], any[Login])) thenReturn Future(Right(Unit))
  when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])) thenReturn Future(Right(Unit))
  when(reportStatusConnectorMock.getByReference(any[String], any[Login])) thenReturn Future(Right(reportStatus))

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[Login](submissionId, LoginId.toString, login)
    new ConfirmationController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, reportStatusConnectorMock)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ConfirmationController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, reportStatusConnectorMock)
  }

  def viewAsString() = confirmation(username, submissionId, frontendAppConfig)(fakeRequest, messages).toString
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
  }
}
