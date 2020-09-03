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



  def submission1 = () => submission_panel(reportStatus1, new govukSummaryList())(messages, FakeRequest())


  "Submission Panel" must {

    "Contain a submission ID equal to 'submissionId'" in {
      lazy val doc = asDocument(submission1())
      val status = doc.select(s"#summary-list-${reportStatus1.id.split("-").head} > div:nth-child(1) > dd").text
      status mustBe submissionId
    }

  }

}
