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

import models.{Done, ReportStatus}
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours

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
    () => createConfirmationView()(username, submissionId)(confirmationFakeRequest, messages)
  def createViewWithStatus =
    () => createConfirmationView()(username, submissionId, Some(reportStatus))(confirmationFakeRequest, messages)

  lazy val doc = asDocument(createView())

  "Confirmation view" must {
    behave like normalPage(createViewWithStatus, messageKeyPrefix, "submission.details")

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.select("body > div > dl > div:nth-child(2) > dd").text
      user mustBe "Bristol"
    }

    "Include a signout link which redirects the users to the signout page" in {
      val href = doc.getElementById("signout-link").attr("href")
      href mustBe controllers.routes.SignOutController.signOut().url
    }

    "Include a print link when completed" in {
      val downloadButton = asDocument(createViewWithStatus()).getElementById("print-button").text
      downloadButton mustBe messages("report.link.print")
    }

  }
}
