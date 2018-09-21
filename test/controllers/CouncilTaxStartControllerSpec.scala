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
import identifiers.{LoginId, VOAAuthorisedId}
import models._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.{councilTaxStart, login}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CouncilTaxStartControllerSpec extends ControllerSpecBase with MockitoSugar {

  val username = "AUser"
  val login = Login("foo", "bar")
  val submissionId = "SId9324832"
  val reportStatus = ReportStatus(submissionId, OffsetDateTime.now, userId = Some(username), status = Some(Submitted.value))

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def loggedInController(
                          reportStatusConnector: ReportStatusConnector,
                          dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap
                        ) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[Login]("", LoginId.toString, login)
    new CouncilTaxStartController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, new FakeNavigator(desiredRoute = onwardRoute), FakeDataCacheConnector, reportStatusConnector)
  }

  def notLoggedInController(
                             reportStatusConnector: ReportStatusConnector,
                             dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap
                           ) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxStartController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, new FakeNavigator(desiredRoute = onwardRoute), FakeDataCacheConnector, reportStatusConnector)
  }

  def reportStatusConnect() = {
    val reportStatusConnector = mock[ReportStatusConnector]
    when(reportStatusConnector.get(any[Login])).thenReturn(Future(Right(Seq(reportStatus))))
    reportStatusConnector
  }
  def reportStatusConnectFailure() = {
    val reportStatusConnector = mock[ReportStatusConnector]
    when(reportStatusConnector.get(any[Login])).thenReturn(Future(Left(Error("error", Seq()))))
    reportStatusConnector
  }

  def viewAsString() = councilTaxStart(username, frontendAppConfig, Seq(reportStatus), Some(reportStatus))(fakeRequest, messages).toString

  "CouncilTaxStart Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController(reportStatusConnect).onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController(reportStatusConnect).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a redirect when calling goToCouncilTaxUploadPage" in {
      val result = loggedInController(reportStatusConnect).goToCouncilTaxUploadPage()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }
  }
}
