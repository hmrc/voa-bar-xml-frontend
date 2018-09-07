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

package connectors

import models.Error
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import reactivemongo.api.ReadPreference
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.BSONObjectID
import repositories.{UserReportUpload, UserReportUploadsReactiveRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UserReportUploadsConnectorSpec extends PlaySpec with MockitoSugar {
  private val reference = "0123456789ab0123456789ab"
  private val invalidReference = "nope"
  private val userName = "foo"
  private val password = "bar"
  private val userReportUpload = UserReportUpload(reference, userName, password)
  private val errorMessage = "error message :("
  private val exception = new Exception(errorMessage)
  private val error = Error(exception.getMessage)
  "DefaultUserReportUploadsConnector" must {
    "have a method that save user and report information that" must {
      "return a successful result when valid arguments are provided" in {
        val writeResult = mock[WriteResult]
        val userReportUploadsReactiveRepositoryMock = mock[UserReportUploadsReactiveRepository]
        when(userReportUploadsReactiveRepositoryMock.insert(userReportUpload)).thenReturn(Future.successful(writeResult))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(userReportUploadsReactiveRepositoryMock)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Right(Unit)
      }
      "return a failed result when the repository fails" in {
        val writeResult = mock[WriteResult]
        val userReportUploadsReactiveRepositoryMock = mock[UserReportUploadsReactiveRepository]
        when(userReportUploadsReactiveRepositoryMock.insert(userReportUpload)).thenReturn(Future.failed(exception))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(userReportUploadsReactiveRepositoryMock)

        val result = await(userReportUploadsRepository.save(userReportUpload))

        result mustBe Left(error)
      }
    }
    "have a method that get user and report information that" must {
      "a successful result when a valid reference id is provided" in {
        val userReportUploadsReactiveRepositoryMock = mock[UserReportUploadsReactiveRepository]
        when(userReportUploadsReactiveRepositoryMock.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext]))
          .thenReturn(Future.successful(Some(userReportUpload)))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(userReportUploadsReactiveRepositoryMock)

        val result = await(userReportUploadsRepository.getByReference(reference))

        result mustBe Right(Some(userReportUpload))
      }
      "a failed result when an invalid reference id is provided" in {
        val userReportUploadsReactiveRepositoryMock = mock[UserReportUploadsReactiveRepository]
        when(userReportUploadsReactiveRepositoryMock.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext]))
          .thenReturn(Future.successful(Some(userReportUpload)))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(userReportUploadsReactiveRepositoryMock)

        val result = await(userReportUploadsRepository.getByReference(invalidReference))

        result mustBe Left(Error(s"$invalidReference could not be parsed as Id" ,Seq()))
      }
      "return a failed result when the repository fails" in {
        val userReportUploadsReactiveRepositoryMock = mock[UserReportUploadsReactiveRepository]
        when(userReportUploadsReactiveRepositoryMock.findById(any[BSONObjectID], any[ReadPreference])(any[ExecutionContext]))
          .thenReturn(Future.failed(exception))
        val userReportUploadsRepository = new DefaultUserReportUploadsConnector(userReportUploadsReactiveRepositoryMock)

        val result = await(userReportUploadsRepository.getByReference(reference))

        result mustBe Left(error)
      }
    }
  }
}
