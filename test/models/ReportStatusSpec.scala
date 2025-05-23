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

package models

import org.scalatestplus.play.PlaySpec

class ReportStatusSpec extends PlaySpec {

  val baCode            = "ba1221"
  val submissionId      = "sId999"
  val reportStatusError = Seq(Error("BAD-CHAR"))

  "ReportStatus model" must {

    "Produce a ReportStatus model with no errors" in {
      val result = ReportStatus(submissionId, baCode = Some(baCode), status = Some("SUBMITTED"))
      result.baCode mustBe Some(baCode)
      result.id mustBe submissionId
      result.status mustBe Some("SUBMITTED")
    }

    "Produce a ReportStatus model with errors" in {
      val result = ReportStatus(submissionId, baCode = Some(baCode), status = Some("INVALIDATED"), errors = reportStatusError)
      result.baCode mustBe Some(baCode)
      result.id mustBe submissionId
      result.status mustBe Some("INVALIDATED")
      result.errors mustBe reportStatusError
    }
  }
}
