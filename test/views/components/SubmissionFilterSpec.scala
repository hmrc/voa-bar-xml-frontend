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

import models.Done
import views.behaviours.ViewBehaviours
import views.html.components.submission_filter

class SubmissionFilterSpec extends ViewBehaviours {
  "Submission Filter component" should {
    "have a failed filter" in {
      val doc = asDocument(submission_filter(messages))

      val element = Option(doc.getElementsContainingText(messages("report.filter.failed")))

      element mustNot be(None)
    }
    "have a complete filter" in {
      val doc = asDocument(submission_filter(messages))

      val element = Option(doc.getElementsContainingText(messages("report.filter.done")))

      element mustNot be(None)
    }
    "have a all filter" in {
      val doc = asDocument(submission_filter(messages, Some(Done.value)))

      val element = Option(doc.getElementsContainingText(messages("report.filter.all")))

      element mustNot be(None)
    }
  }
}
