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

import play.api.test.Helpers._
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction}
import play.api.mvc.MessagesControllerComponents
import viewmodels.AnswerSection

import scala.concurrent.ExecutionContext

class CheckYourAnswersControllerSpec extends ControllerSpecBase {

  def ec                   = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  def checkYourAnswerView  = app.injector.instanceOf[views.html.check_your_answers]

  def controller(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) =
    new CheckYourAnswersController(
      frontendAppConfig,
      messagesApi,
      dataRetrievalAction,
      new DataRequiredActionImpl(ec),
      checkYourAnswerView,
      controllerComponents
    )

  "Check Your Answers Controller" must {
    "return 200 and the correct view for a GET" in {
      val result = controller().onPageLoad()(fakeRequest)
      status(result) mustBe OK
      contentAsString(result) mustBe checkYourAnswerView(frontendAppConfig, Seq(AnswerSection(None, Seq())))(using fakeRequest, messages).toString
    }

    "redirect to Session Expired for a GET if not existing data is found" in {
      val result = controller(dontGetAnyData).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SessionExpiredController.onPageLoad.url)
    }
  }
}
