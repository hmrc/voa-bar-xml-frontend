/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._

class TimeOutControllerSpec extends ControllerSpecBase {

  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  def sessionTimeout = app.injector.instanceOf[views.html.session_timeout]

  "Timeout Controller" must {
    "return 200 for a GET /this-service-has-been-reset" in {
      val result = new TimeoutController(controllerComponents, sessionTimeout).onPageLoad()(fakeRequest)
      status(result) mustBe OK
    }

    "return 303 for a GET /this-service-has-been-reset/redirect" in {
      val result = new TimeoutController(controllerComponents, sessionTimeout).timeout()(fakeRequest)
      status(result) mustBe SEE_OTHER
    }
  }
}
