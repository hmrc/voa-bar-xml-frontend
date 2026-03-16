/*
 * Copyright 2026 HM Revenue & Customs
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
import views.behaviours.ViewBehaviours

class WelcomeViewSpec extends ViewBehaviours:

  private def welcome = inject[views.html.welcome]

  private val username         = "BA0505"
  private val messageKeyPrefix = "welcome"

  private val welcomeFakeRequest = fakeRequest

  private def createView = () => welcome(frontendAppConfig, username)(using welcomeFakeRequest, messages)

  "Welcome view" must {
    behave like normalPage(createView, messageKeyPrefix)
  }

  "The Council Tax links to the goToCouncilTaxUploadPage method" in {
    val doc  = asDocument(createView())
    val href = doc.getElementById("councilTaxLink").attr("href")
    assert(href == controllers.routes.WelcomeController.goToCouncilTaxUploadPage.url)
  }

  // Welcome page containing form for navigation

  private def createFormView(formUser: String) = () => welcome(frontendAppConfig, formUser)(using welcomeFakeRequest, messages)

  def uploadLinkTest(ba: String, formDoc: Document): Unit =
    s"The upload link to the goToCouncilTaxUploadPage method for $ba" in {
      val href = formDoc.getElementById("councilTaxLink").attr("href")
      assert(href == controllers.routes.WelcomeController.goToCouncilTaxUploadPage.url)
    }

  def viewHistoryTest(ba: String, formDoc: Document): Unit =
    s"The view history link to the ReportStatusController.onPageLoad method for $ba" in {
      val href = formDoc.getElementById("submissions").attr("href")
      assert(href == controllers.routes.ReportStatusController.onPageLoad().url)
    }

  def runFormNavigationTests(ba: String): Unit =
    lazy val formDoc = asDocument(createFormView(ba)())

    s"The webform link is not visible for $ba" in
      assertThrows[NullPointerException](formDoc.getElementById("create").attr("href"))

    uploadLinkTest(ba, formDoc)
    viewHistoryTest(ba, formDoc)

  private val baCodes: Seq[String] = Seq("BA0114", "BA5960")

  baCodes.foreach(runFormNavigationTests)

  def runPilotBATests(ba: String): Unit =
    lazy val formDoc = asDocument(createFormView(ba)())

    s"The webform link is visible for $ba" in {
      val href = formDoc.getElementById("create").attr("href")
      assert(href == controllers.routes.ReportReasonController.onPageLoad.url)
    }

    uploadLinkTest(ba, formDoc)
    viewHistoryTest(ba, formDoc)

  val pilotBaCodes: Seq[String] = Seq("BA1445", "BA3615", "BA3630", "BA3650", "BA3810")

  pilotBaCodes.foreach(runPilotBATests)
