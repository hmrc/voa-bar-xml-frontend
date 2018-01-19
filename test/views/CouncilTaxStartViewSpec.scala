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
import views.html.councilTaxStart

class CouncilTaxStartViewSpec extends ViewBehaviours {

  val messageKeyPrefix = "councilTaxStart"

  def createView = () => councilTaxStart(frontendAppConfig)(fakeRequest, messages)

  "CouncilTaxStart view" must {
    behave like normalPage(createView, messageKeyPrefix, "paragraph", "submissions.url-title")
  }


  "contain Start button with the value Start now" in {
    val doc = asDocument(createView())
    val loginButton = doc.getElementById("start").text()
    assert(loginButton == messages("site.start"))
  }

  "The Council Tax Start now button goes to the goToCouncilTaxUploadPage method" in {
    val doc = asDocument(createView())
    val href = doc.getElementById("start").attr("href")
    assert(href == controllers.routes.CouncilTaxStartController.goToCouncilTaxUploadPage().url.toString)
  }
}
