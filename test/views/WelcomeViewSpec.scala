/*
 * Copyright 2021 HM Revenue & Customs
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

import org.jsoup.nodes.Document
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours

class WelcomeViewSpec extends ViewBehaviours {

  val username = "BA0505"
  val messageKeyPrefix = "welcome"
  val cr05FeatureFlag = false

  val welcomeFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.WelcomeController"))

  def createView = () => createWelcomeView()(frontendAppConfig, username, cr05FeatureFlag)(welcomeFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Welcome view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "The Council Tax links to the goToCouncilTaxUploadPage method" in {
    val doc = asDocument(createView())
    val href = doc.getElementById("councilTaxLink").attr("href")
    assert(href == controllers.routes.WelcomeController.goToCouncilTaxUploadPage().url.toString)
  }

  // Welcome page containing form for navigation

  def createFormView(formUser: String) = () => createWelcomeView()(frontendAppConfig, formUser, cr05FeatureFlag)(welcomeFakeRequest, messages)

  def uploadLinkTest(ba: String, formDoc: Document) = {
    s"The upload link to the goToCouncilTaxUploadPage method for ${ba}" in {
      val href = formDoc.getElementById("councilTaxLink").attr("href")
      assert(href == controllers.routes.WelcomeController.goToCouncilTaxUploadPage().url.toString)
    }
  }

  def viewHistoryTest(ba: String, formDoc: Document) = {
    s"The view history link to the ReportStatusController.onPageLoad method for ${ba}" in {
      val href = formDoc.getElementById("submissions").attr("href")
      assert(href == controllers.routes.ReportStatusController.onPageLoad().url.toString)
    }
  }

  def runFormNavigationTests(ba: String) = {
    lazy val formDoc = asDocument(createFormView(ba)())

    s"The webform link is not visible for ${ba}" in {
      assertThrows[NullPointerException] {formDoc.getElementById("create").attr("href")}
    }

    uploadLinkTest(ba, formDoc)
    viewHistoryTest(ba, formDoc)
  }

  val baCodes = Seq("BA0114", "BA5960")

  baCodes.foreach(
    runFormNavigationTests(_)
  )

  def runPilotBATests(ba: String) = {
    lazy val formDoc = asDocument(createFormView(ba)())

    s"The webform link is visible for ${ba}" in {
      val href = formDoc.getElementById("create").attr("href")
      assert(href == controllers.routes.WelcomeController.goToStartWebFormPage().url.toString)
    }

    uploadLinkTest(ba, formDoc)
    viewHistoryTest(ba, formDoc)
  }

  val pilotBaCodes = Seq("BA1445", "BA3615", "BA3630", "BA3650", "BA3810")

  pilotBaCodes.foreach(
    runPilotBATests(_)
  )

  // TODO https://jira.tools.tax.service.gov.uk/browse/VOA-2065 test link to upload page
}
