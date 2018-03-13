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
import forms.FileUploadDataFormProvider
import models.NormalMode
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours
import views.html.councilTaxUpload

class CouncilTaxUploadViewSpec extends ViewBehaviours {

  val username = "BA0345"
  val messageKeyPrefix = "councilTaxUpload"

  val form = new FileUploadDataFormProvider()()

  val councilTaxUploadFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.CouncilTaxUploadController"))

  def createView = () => councilTaxUpload(username, frontendAppConfig, form)(councilTaxUploadFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "CouncilTaxUpload view" must {
    behave like normalPage(createView, messageKeyPrefix, "info.format", "info.multi", "info.upload", "info.size", "info.files", "message1",
      "message2", "xml")

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.getElementById("username-element").text
      user mustBe "Reading"
    }

    "Include a logout link which redirects the users to the login page" in {
      val href = doc.getElementById("logout-link").attr("href")
      href mustBe controllers.routes.LoginController.onPageLoad(NormalMode).url.toString
    }

    "Have a home link in the top navigation bar which links to the welcome page and display '> Upload' next to the home link" in {
      val href = doc.getElementById("homelink").attr("href")
      val currentPageName = doc.getElementById("upload-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> Upload"
    }

    "contain Submit button with the value Submit" in {
      val doc = asDocument(createView())
      val submitButton = doc.getElementById("submit").text()
      submitButton mustBe messages("site.submit")
    }
  }
}
