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

package views

import models.NormalMode
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours
import views.html.welcome

class WelcomeViewSpec extends ViewBehaviours {

  val username = "BA0505"
  val messageKeyPrefix = "welcome"

  val welcomeFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.WelcomeController"))

  def createView = () => welcome(username, frontendAppConfig)(welcomeFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Welcome view" must {
    behave like normalPage(createView, messageKeyPrefix, "councilTax.url", "councilTax.description")
  }

  "The Council Tax links to the goToCouncilTaxStartPage method" in {
    val doc = asDocument(createView())
    val href = doc.getElementById("councilTaxLink").attr("href")
    assert(href == controllers.routes.WelcomeController.goToCouncilTaxStartPage().url.toString)
  }

  "Include an username element displaying the BA name based on given BA Code" in {
    val user = doc.getElementById("username-element").text
    user mustBe "Cambridge"
  }

  "Include a logout link which redirects the users to the login page" in {
    val href = doc.getElementById("logout-link").attr("href")
    href mustBe controllers.routes.LoginController.onPageLoad(NormalMode).url.toString
  }

  "Have a home-element in the top navigation bar" in {
    val elem = doc.getElementById("home-element").text
    elem mustBe "Home"
  }
}
