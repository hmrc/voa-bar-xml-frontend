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
package repositories

import org.scalatestplus.play.PlaySpec
import scala.concurrent.ExecutionContext.Implicits.global

class UserReportUploadsRepositorySpec extends PlaySpec {
  "DefaultUserReportUploadsRepository" must {
    "have a method that save user and report information" when {
      "valid arguments are provided" in {
        val userReportUploadsRepository = new DefaultUserReportUploadsRepository(???)

        userReportUploadsRepository.save(???)
      }
    }
    "have a method that get user and report information" when {
      "a valid reference id is provided" in {
        val userReportUploadsRepository = new DefaultUserReportUploadsRepository(???)

        userReportUploadsRepository.getByReference(???)
      }
    }
  }
}
