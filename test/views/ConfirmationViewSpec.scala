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

package views

import controllers.routes
import models.NormalMode
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours
import views.html.confirmation

class ConfirmationViewSpec extends ViewBehaviours {

  val username = "BA0116"
  val messageKeyPrefix = "confirmation"
  val confirmationFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.ConfirmationController"))

  def createView = () => confirmation(username, frontendAppConfig)(confirmationFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Confirmation view" must {
    behave like normalPage(createView, messageKeyPrefix)

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.getElementById("username-element").text
      user mustBe "Bristol"
    }

    "Include a logout link which redirects the users to the login page" in {
      val href = doc.getElementById("logout-link").attr("href")
      href mustBe controllers.routes.LoginController.onPageLoad(NormalMode).url.toString
    }

    "Have a home link in the top navigation bar which links to the welcome page and display '> Confirmation' next to the home link" in {
      val href = doc.getElementById("homelink").attr("href")
      val currentPageName = doc.getElementById("confirmation-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> Confirmation"
    }
  }
}
