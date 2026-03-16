/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.FakeDataCacheConnector
import controllers.actions.*
import identifiers.VOAuthorisedId
import models.NormalMode
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import utils.FakeNavigator
import views.ViewSpecBase
import scala.concurrent.ExecutionContext

class WelcomeControllerSpec extends ControllerSpecBase with ViewSpecBase:

  private val username             = "AUser"
  private val welcome              = inject[views.html.welcome]
  private val ec                   = inject[ExecutionContext]
  private val controllerComponents = inject[MessagesControllerComponents]

  private def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  private def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAuthorisedId.toString, username)
    WelcomeController(
      frontendAppConfig,
      dataRetrievalAction,
      DataRequiredActionImpl(ec),
      FakeNavigator(desiredRoute = onwardRoute),
      FakeDataCacheConnector,
      controllerComponents,
      welcome
    )(using ec)

  private def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap): WelcomeController =
    FakeDataCacheConnector.resetCaptures()
    WelcomeController(
      frontendAppConfig,
      dataRetrievalAction,
      DataRequiredActionImpl(ec),
      FakeNavigator(desiredRoute = onwardRoute),
      FakeDataCacheConnector,
      controllerComponents,
      welcome
    )(using ec)

  private def viewAsString() = welcome(frontendAppConfig, username)(using fakeRequest, messages).toString

  "Welcome Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VO must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a redirect when calling goToCouncilTaxUploadPage" in {
      val result = loggedInController().goToCouncilTaxUploadPage()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }

    "return a redirect when calling goToTaskListLoadPage" in {
      val result = loggedInController().goToTaskListLoadPage()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }
  }
