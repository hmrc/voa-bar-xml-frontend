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

package models

import org.scalatestplus.play.PlaySpec

class ReportStatusSpec extends PlaySpec {

  val baCode = "ba1221"
  val submissionId = "sId999"
  val errors = Seq(Error("BAD-CHAR", Seq("ba1221")))

  "ReportStatus model" must {

    "Produce a ReportStatus model with no errors" in {
      val result = ReportStatus(baCode, submissionId, "SUBMITTED")
      result.baCode mustBe baCode
      result.submissionId mustBe submissionId
      result.status mustBe "SUBMITTED"
    }

    "Produce a ReportStatus model with errors" in {
      val result = ReportStatus(baCode, submissionId, "INVALIDATED", errors)
      result.baCode mustBe baCode
      result.submissionId mustBe submissionId
      result.status mustBe "INVALIDATED"
      result.errors mustBe errors
    }
  }
}
