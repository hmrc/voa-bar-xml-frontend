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

import base.SpecBase
import models.{Error, Login, UserReportUpload}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UserReportUploadsConnectorSpec extends PlaySpec with MockitoSugar with SpecBase:

  private val reference                  = "0123456789ab0123456789ab"
  private val userName                   = "foo"
  private val password                   = "bar"
  private val userReportUpload           = UserReportUpload(reference, userName, password)
  private val errorMessage               = "error message :("
  private val exception                  = new Exception(errorMessage)
  private val error                      = Error(exception.getMessage)
  private val servicesConfig             = inject[ServicesConfig]
  private val login                      = Login("foo", "bar")
  implicit private val hc: HeaderCarrier = HeaderCarrier()

  "DefaultUserReportUploadsConnector" must {
    "have a method that save user and report information that" must {
      "return a successful result when valid arguments are provided" in {
        val httpClientV2Mock = mock[HttpClientV2]
        when(
          httpClientV2Mock.put(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Right(OK), "{}"))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2Mock, servicesConfig)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Right(())
      }

      "return a failed result when the repository fails" in {
        val httpClientV2FailedMock = mock[HttpClientV2]
        when(
          httpClientV2FailedMock.put(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Left(exception)))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2FailedMock, servicesConfig)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Left(error)
      }
    }
    "have a method that get user and report information that"  must {
      "a successful result when a valid reference id is provided" in {
        val httpClientV2Mock = mock[HttpClientV2]
        when(
          httpClientV2Mock.get(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Right(OK), Json.toJson(userReportUpload).toString))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2Mock, servicesConfig)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Right(Some(userReportUpload))
      }

      "handle empty response if user report doesn't exist" in {
        val httpClientV2Mock = mock[HttpClientV2]
        when(
          httpClientV2Mock.get(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Right(OK)))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2Mock, servicesConfig)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Right(None)
      }

      "handle empty json response if user report doesn't exist" in {
        val httpClientV2Mock = mock[HttpClientV2]
        when(
          httpClientV2Mock.get(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Right(OK), "{}"))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2Mock, servicesConfig)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Right(None)
      }

      "return a failed result when the repository fails" in {
        val httpClientV2FailedMock = mock[HttpClientV2]
        when(
          httpClientV2FailedMock.get(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Left(exception)))

        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpClientV2FailedMock, servicesConfig)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Left(error)
      }
    }
  }
