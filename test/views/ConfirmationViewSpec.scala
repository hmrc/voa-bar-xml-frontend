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

import models.{Done, ReportStatus}
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.behaviours.ViewBehaviours
import views.html.confirmation

class ConfirmationViewSpec extends ViewBehaviours:

  private def confirmation: confirmation = inject[views.html.confirmation]

  private val username         = "BA0116"
  private val submissionId     = "SId328473"
  private val messageKeyPrefix = "confirmation"

  private val confirmationFakeRequest = FakeRequest(GET, "/service-root/some-page")

  private val reportStatus = ReportStatus(
    submissionId,
    status = Some(Done.value)
  )

  private def createView = () => confirmation(username, submissionId)(using confirmationFakeRequest, messages)

  private def createViewWithStatus = () => confirmation(username, submissionId, Some(reportStatus))(using confirmationFakeRequest, messages)

  private val doc: Document = asDocument(createView())

  "Confirmation view" must {
    behave like normalPage(createViewWithStatus, messageKeyPrefix, "submission.details")

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.select("#account-info-header > li:nth-child(2) > span:nth-child(2)").text
      user mustBe "Bristol"
    }

    "Include a signout link which redirects the users to the signout page" in {
      val href = doc.getElementsByClass("hmrc-sign-out-nav__link").first.attr("href")
      href mustBe controllers.routes.SignOutController.signOut.url
    }

    "Include a print link when completed" in {
      val downloadButton = asDocument(createViewWithStatus()).getElementById("print-button").text
      downloadButton mustBe messages("report.link.print")
    }

  }
