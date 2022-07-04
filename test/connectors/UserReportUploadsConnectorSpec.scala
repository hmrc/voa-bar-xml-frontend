/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.scalatest.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Writes
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UserReportUploadsConnectorSpec extends PlaySpec with MockitoSugar with SpecBase {
  private val reference = "0123456789ab0123456789ab"
  private val userName = "foo"
  private val password = "bar"
  private val userReportUpload = UserReportUpload(reference, userName, password)
  private val errorMessage = "error message :("
  private val exception = new Exception(errorMessage)
  private val error = Error(exception.getMessage)
  private val httpResponse = mock[HttpResponse]
  private lazy val configuration = injector.instanceOf(classOf[Configuration])
  private val login = Login("foo", "bar")
  implicit private val hc = HeaderCarrier()

  "DefaultUserReportUploadsConnector" must {
    "have a method that save user and report information that" must {
      "return a successful result when valid arguments are provided" in {
        val httpMock = mock[HttpClient]
        when(httpMock.PUT(any[String], any[UserReportUpload], any[Seq[(String,String)]])
          (any[Writes[UserReportUpload]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(httpResponse))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpMock, configuration)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Right(())
      }
      "return a failed result when the repository fails" in {
        val httpMock = mock[HttpClient]
        when(httpMock.PUT(any[String], any[UserReportUpload], any[Seq[(String,String)]])
        (any[Writes[UserReportUpload]], any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.failed(exception))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpMock, configuration)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Left(error)
      }
    }
    "have a method that get user and report information that" must {
      "a successful result when a valid reference id is provided" in {
        val httpMock = mock[HttpClient]
        when(httpMock.GET[Option[UserReportUpload]](any[String], anySeq, anySeq)(
          any[HttpReads[Option[UserReportUpload]]], any[HeaderCarrier], any[ExecutionContext]))
            .thenAnswer(Future.successful(Some(userReportUpload)))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpMock, configuration)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Right(Some(userReportUpload))
      }
      "return a failed result when the repository fails" in {
        val httpMock = mock[HttpClient]
        when(httpMock.GET[Option[UserReportUpload]](any[String], anySeq, anySeq)
          (any[HttpReads[Option[UserReportUpload]]], any[HeaderCarrier], any[ExecutionContext]))
            .thenReturn(Future.failed(exception))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(httpMock, configuration)

        val result = await(userReportUploadsRepository.getById(reference, login))

        result mustBe Left(error)
      }
    }
  }
}
