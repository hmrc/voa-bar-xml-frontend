/*
 * Copyright 2024 HM Revenue & Customs
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
import views.behaviours.ViewBehaviours

class ConfirmationViewSpec extends ViewBehaviours {

  def confirmation = app.injector.instanceOf[views.html.confirmation]

  val username                = "BA0116"
  val submissionId            = "SId328473"
  val messageKeyPrefix        = "confirmation"
  val confirmationFakeRequest = fakeRequest

  val reportStatus = ReportStatus(
    submissionId,
    status = Some(Done.value)
  )

  def createView =
    () => confirmation(username, submissionId)(using confirmationFakeRequest, messages)

  def createViewWithStatus =
    () => confirmation(username, submissionId, Some(reportStatus))(using confirmationFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Confirmation view" must {
    behave like normalPage(createViewWithStatus, messageKeyPrefix, "submission.details")

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.select("body > div > dl > div:nth-child(2) > dd").text
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
}
