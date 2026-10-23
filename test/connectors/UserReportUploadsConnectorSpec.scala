/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors

import models.{Error, Login, UserReportUpload}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.vo.unit.test.BaseAppSpec

class UserReportUploadsConnectorSpec extends BaseAppSpec:

  private val reference        = "0123456789ab0123456789ab"
  private val userName         = "foo"
  private val password         = "bar"
  private val userReportUpload = UserReportUpload(reference, userName, password)
  private val errorMessage     = "error message :("
  private val exception        = Exception(errorMessage)
  private val error            = Error(exception.getMessage)
  private val login            = Login("foo", "bar")

  "DefaultUserReportUploadsConnector" should {
    "have a method that save user and report information that" should {
      "return a successful result when valid arguments are provided" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientMock(PUT, responseBody = "{}"), servicesConfig)

        val result = userReportUploadsRepository.save(userReportUpload).futureValue

        result shouldBe Right(())
      }

      "return a failed result when the repository fails" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientFailedMock(PUT, returnFailure = exception), servicesConfig)

        val result = userReportUploadsRepository.save(userReportUpload).futureValue

        result shouldBe Left(error)
      }
    }

    "have a method that get user and report information that" should {
      "a successful result when a valid reference id is provided" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientMock(responseBody = Json.toJson(userReportUpload)), servicesConfig)

        val result = userReportUploadsRepository.getById(reference, login).futureValue

        result shouldBe Right(Some(userReportUpload))
      }

      "handle empty response if user report doesn't exist" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientMock(responseBody = "{}"), servicesConfig)

        val result = userReportUploadsRepository.getById(reference, login).futureValue

        result shouldBe Right(None)
      }

      "handle empty json response if user report doesn't exist" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientMock(responseBody = "{}"), servicesConfig)

        val result = userReportUploadsRepository.getById(reference, login).futureValue

        result shouldBe Right(None)
      }

      "return a failed result when the repository fails" in {
        val userReportUploadsRepository = DefaultUserReportUploadsConnector(httpClientFailedMock(returnFailure = exception), servicesConfig)

        val result = userReportUploadsRepository.getById(reference, login).futureValue

        result shouldBe Left(error)
      }
    }
  }
