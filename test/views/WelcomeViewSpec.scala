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

import views.behaviours.ViewBehaviours
import views.html.welcome

class WelcomeViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "welcome"

  def createView = () => welcome(frontendAppConfig)(fakeRequest, messages)

  "Welcome view" must {
    behave like normalPage(createView, messageKeyPrefix, "councilTax.url", "councilTax.description")
  }

  "The Council Tax links to the goToCouncilTaxStartPage method" in {
    val doc = asDocument(createView())
    val href = doc.getElementById("councilTaxLink").attr("href")
    assert(href == controllers.routes.WelcomeController.goToCouncilTaxStartPage().url.toString)
  }
}
