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

import java.time.ZonedDateTime

import controllers.routes
import models.{Done, NormalMode, ReportStatus}
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours
import views.html.confirmation

class ConfirmationViewSpec extends ViewBehaviours {

  val username = "BA0116"
  val submissionId = "SId328473"
  val messageKeyPrefix = "confirmation"
  val confirmationFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.ConfirmationController"))
  val reportStatus = ReportStatus(
    submissionId,
    ZonedDateTime.now,
    status = Some(Done.value)
  )

  def createView =
    () => confirmation(username, submissionId, frontendAppConfig)(confirmationFakeRequest, messages)
  def createViewWithStatus =
    () => confirmation(username, submissionId, frontendAppConfig, Some(reportStatus))(confirmationFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Confirmation view" must {
    behave like normalPage(createView, messageKeyPrefix, "subheading", "file.received", "window.open", "options.title",
      "options.another", "options.history", "submissionId")

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
      currentPageName mustBe "Submission"
    }

    "Include an upload link which redirects the users to council tax upload page" in {
      val href = doc.getElementById("upload-link").attr("href")
      href mustBe controllers.routes.CouncilTaxUploadController.onPageLoad().url.toString
    }

    "Include a history link which redirects the users to council tax submissions history page" in {
      val href = doc.getElementById("history-link").attr("href")
      href mustBe controllers.routes.ReportStatusController.onPageLoad().url.toString
    }

    "Include a Download receipt button when completed" in {
      val downloadButton = asDocument(createViewWithStatus()).getElementById("print-button").text
      downloadButton mustBe messages("site.print.button")
    }

    "Include a hidden VOA Logo and have the aria-hidden attribute set to true" in {
      val logoHiddenAttribute = doc.getElementsByClass("voa-logo").attr("aria-hidden")
      logoHiddenAttribute mustBe "true"
    }
  }
}
