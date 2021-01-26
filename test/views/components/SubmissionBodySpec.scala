/*
 * Copyright 2021 HM Revenue & Customs
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
import views.html.components.submission_body

class SubmissionBodySpec extends ViewBehaviours {

  val submissionId = "SId9324832"
  val baCode = "baCode"
  val reportStatus = ReportStatus(submissionId, ZonedDateTime.now, baCode = Some(baCode), status = Some(Submitted.value))

  def submission = () => submission_body(reportStatus)(messages)

  lazy val doc = asDocument(submission())

  "Submission Body " must {

    "Contain the following keys" in {
      assertContainsText(doc, messages("submission"))
      assertContainsText(doc, messages("status.type.title"))
      assertContainsText(doc, messages("status.created.para1"))
      assertContainsText(doc, messages("status.created.para1"))
      assertContainsText(doc, messages("site.print.button"))
    }
  }

}
