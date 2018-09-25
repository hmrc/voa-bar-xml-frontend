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

import java.time.ZonedDateTime

import models.{ReportStatus, Submitted}
import views.behaviours.ViewBehaviours
import views.html.components.submission_block

class SubmissionBlockSpec extends ViewBehaviours {

  val submissionId = "SId9324832"
  val reportStatus = ReportStatus(submissionId, ZonedDateTime.now, status = Some(Submitted.value), baCode = Some("BA0121"))

  def submission = () => submission_block(reportStatus)(messages)
  lazy val doc = asDocument(submission())

  "Submission Block " must {

    "Contain a submission block div of type bordered-box" in {
      val sBlock = doc.getElementById("submission-block").className
      sBlock mustBe "bordered-box"
    }

    "Contain a submission id" in {
      val sId = doc.getElementById("submission-id").text
      sId mustBe messages("status.submissionId.title") + s" $submissionId"
    }

    "Contain a submission panel if the report statuses is not empty" in {
      val panels = doc.getElementsByClass("submission-panel")
      panels.size mustBe 1
    }
  }

}
