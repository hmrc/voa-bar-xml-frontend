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

import models.{Done, Failed, Pending, ReportStatus, Submitted, Verified}
import play.routing.Router.Tags.ROUTE_CONTROLLER
import views.behaviours.ViewBehaviours

class ReportStatusViewSpec extends ViewBehaviours with ViewSpecBase {

  val username = "BA0350"
  val messageKeyPrefix = "reportStatus"
  val submissionId = "SId9324832"
  val baCode = "baCode"
  val date = ZonedDateTime.now
  val reportStatus1 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Submitted.value))
  val reportStatus2 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Verified.value))
  val reportStatus3 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Failed.value))
  val reportStatus4 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Done.value))
  val reportStatus5 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Pending.value))


  val reportStatusFakeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.ReportStatusController"))

  def createView(reportStatuses: Seq[ReportStatus] = Seq()) = () => createReportStatusView()(username, reportStatuses)(reportStatusFakeRequest, messages)

  def doc(reportStatuses: Seq[ReportStatus] = Seq()) = asDocument(createView(reportStatuses)())

  "ReportStatus view" must {
    behave like normalPage(createView(), messageKeyPrefix)

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc().select("body > div > dl > div:nth-child(2) > dd").text
      user mustBe "Slough"
    }

    "Include a signout link which redirects the users to the login page" in {
      val href = doc().getElementById("signout-link").attr("href")
      href mustBe controllers.routes.SignOutController.signOut().url
    }

    "Contain a submission title equal to 'SUBMITTED' if the report status is submitted" in {

      val status = doc(Seq(reportStatus1)).select("#main-content > div > div > dl:nth-child(5) > div:nth-child(1) > dd").text
      status mustBe messages("report.status.submitted.title")
    }

    "Contain a submission title equal to 'VERIFIED' if the report status is validated" in {
      val status = doc(Seq(reportStatus2)).select("#main-content > div > div > dl:nth-child(5) > div:nth-child(1) > dd").text
      status mustBe messages("report.status.verified.title")
    }

    "Contain a submission title equal to 'FAILED' if the report status is invalidated" in {
      val status = doc(Seq(reportStatus3)).select("#main-content > div > div > dl:nth-child(5) > div:nth-child(1) > dd").text
      status mustBe messages("report.status.failed.title")
    }

    "Contain a submission title equal to 'DONE' if the report status is forwarded" in {
      val status = doc(Seq(reportStatus4)).select("#main-content > div > div > dl:nth-child(5) > div:nth-child(1) > dd").text
      status mustBe messages("report.status.done.title")
    }

    "Contain a submission title equal to 'PENDING' if the report status is forwarded" in {
      val status = doc(Seq(reportStatus5)).select("#main-content > div > div > dl:nth-child(5) > div:nth-child(1) > dd").text
      status mustBe messages("report.status.pending.title")
    }

  }
}
