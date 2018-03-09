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

import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.VOAAuthorisedId
import models.NormalMode
import play.api.data.Form
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.councilTaxUpload

class CouncilTaxUploadControllerSpec extends ControllerSpecBase {

  val username = "AUser"

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  val formProvider = new FileUploadDataFormProvider()
  val form = formProvider()

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider,new FakeNavigator(desiredRoute = onwardRoute))
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute))
  }

  def viewAsString(form: Form[_] = form) = councilTaxUpload(username, frontendAppConfig, form)(fakeRequest, messages).toString

  "CouncilTaxUpload Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }
  }
}
