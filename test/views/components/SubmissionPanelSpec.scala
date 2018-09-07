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

package views.components

import java.time.OffsetDateTime

import models.ReportStatus
import views.behaviours.ViewBehaviours
import views.html.components.submission_panel

class SubmissionPanelSpec extends ViewBehaviours {

  val submissionId = "SId9324832"
  val date = OffsetDateTime.now
  val reportStatus1 = ReportStatus(submissionId, date, status = Some("SUBMITTED"))
  val reportStatus2 = ReportStatus(submissionId, date, status = Some("VALIDATED"))
  val reportStatus3 = ReportStatus(submissionId, date, status = Some("INVALIDATED"))
  val reportStatus4 = ReportStatus(submissionId, date, status = Some("FORWARDED"))


  def submission1 = () => submission_panel(reportStatus1)(messages)
  def submission2 = () => submission_panel(reportStatus2)(messages)
  def submission3 = () => submission_panel(reportStatus3)(messages)
  def submission4 = () => submission_panel(reportStatus4)(messages)

  "Submission Panel" must {

    "Contain a submission title equal to 'SUBMITTED' if the report status is submitted" in {
      lazy val doc = asDocument(submission1())
      val status = doc.getElementById("submission-title").text
      status mustBe messages("status.submitted.title")
    }

    "Contain a submission title equal to 'VALIDATED' if the report status is validated" in {
      lazy val doc = asDocument(submission2())
      val status = doc.getElementById("submission-title").text
      status mustBe messages("status.validated.title")
    }

    "Contain a submission title equal to 'INVALIDATED' if the report status is invalidated" in {
      lazy val doc = asDocument(submission3())
      val status = doc.getElementById("submission-title").text
      status mustBe messages("status.invalidated.title")
    }

    "Contain a submission title equal to 'FORWARDED' if the report status is forwarded" in {
      lazy val doc = asDocument(submission4())
      val status = doc.getElementById("submission-title").text
      status mustBe messages("status.forwarded.title")
    }
  }

}
