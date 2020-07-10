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

import forms.SubmissionTypeFormProvider
import models.NormalMode
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.govuk.QuestionViewBehaviours

class WelcomeViewSpec extends QuestionViewBehaviours[String] {

  val username = "BA0505"
  val messageKeyPrefix = "welcome"

  val welcomeFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.WelcomeController"))

  def createView = () => createWelcomeView()(frontendAppConfig, NormalMode, username)(welcomeFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Welcome view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "The Council Tax links to the goToCouncilTaxStartPage method" in {
    val doc = asDocument(createView())
    val href = doc.getElementById("councilTaxLink").attr("href")
    assert(href == controllers.routes.WelcomeController.goToCouncilTaxStartPage().url.toString)
  }

  // Welcome page containing form for navigation

  val form = SubmissionTypeFormProvider()(messages)
  val formUsername = "BA1445"
  val formMessageKeyPrefix = "submissionCategory"

  def createFormView = () => createWelcomeView()(frontendAppConfig, NormalMode, formUsername)(welcomeFakeRequest, messages)

  lazy val formDoc = asDocument(createFormView())

  "The view history links to the WelcomeController.goToStartWebFormPage() method" in {
    val href = formDoc.getElementById("create").attr("href")
    assert(href == controllers.routes.WelcomeController.goToStartWebFormPage().url.toString)
  }

  "The view history links to the ReportStatusController.onPageLoad method" in {
    val href = formDoc.getElementById("submissions").attr("href")
    assert(href == controllers.routes.ReportStatusController.onPageLoad().url.toString)
  }

  // TODO https://jira.tools.tax.service.gov.uk/browse/VOA-2065 test link to upload page
}
