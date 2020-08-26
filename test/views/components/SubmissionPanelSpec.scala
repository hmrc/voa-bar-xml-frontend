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

package views.components

import java.time.ZonedDateTime

import models._
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.html.components.govukSummaryList
import views.behaviours.ViewBehaviours
import views.html.components.submission_panel

class SubmissionPanelSpec extends ViewBehaviours  {

  val submissionId = "SId9324832"
  val baCode = "baCode"
  val date = ZonedDateTime.now
  val reportStatus1 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Submitted.value))
  val reportStatus2 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Verified.value))
  val reportStatus3 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Failed.value))
  val reportStatus4 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Done.value))
  val reportStatus5 = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Pending.value))


  def submission1 = () => submission_panel(reportStatus1, new govukSummaryList())(messages, FakeRequest())
  def submission2 = () => submission_panel(reportStatus2, new govukSummaryList())(messages, FakeRequest())
  def submission3 = () => submission_panel(reportStatus3, new govukSummaryList())(messages, FakeRequest())
  def submission4 = () => submission_panel(reportStatus4, new govukSummaryList())(messages, FakeRequest())
  def submission5 = () => submission_panel(reportStatus5, new govukSummaryList())(messages, FakeRequest())

  "Submission Panel" must {

    "Contain a submission title equal to 'SUBMITTED' if the report status is submitted" in {
      lazy val doc = asDocument(submission1())
      val status = doc.select(s"#summary-list-${reportStatus1.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe messages("report.status.submitted.title")
    }

    "Contain a submission title equal to 'VERIFIED' if the report status is validated" in {
      lazy val doc = asDocument(submission2())
      val status = doc.select(s"#summary-list-${reportStatus2.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe messages("report.status.verified.title")
    }

    "Contain a submission title equal to 'FAILED' if the report status is invalidated" in {
      lazy val doc = asDocument(submission3())
      val status = doc.select(s"#summary-list-${reportStatus3.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe messages("report.status.failed.title")
    }

    "Contain a submission title equal to 'DONE' if the report status is forwarded" in {
      lazy val doc = asDocument(submission4())
      val status = doc.select(s"#summary-list-${reportStatus4.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe messages("report.status.done.title")
    }

    "Contain a submission title equal to 'PENDING' if the report status is forwarded" in {
      lazy val doc = asDocument(submission5())
      val status = doc.select(s"#summary-list-${reportStatus5.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe messages("report.status.pending.title")
    }
  }

}
