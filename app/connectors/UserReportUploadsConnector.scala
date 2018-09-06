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

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.Error
import reactivemongo.api.ReadPreference
import reactivemongo.bson.BSONObjectID
import repositories.{UserReportUpload, UserReportUploadsReactiveRepository}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultUserReportUploadsConnector @Inject() (
                                                     userReportUploadsReactiveRepository: UserReportUploadsReactiveRepository
                                                   )(implicit executionContext: ExecutionContext) extends  UserReportUploadsConnector {
  override def save(userReportUpload: UserReportUpload): Future[Either[Error, Unit.type]] = {
    userReportUploadsReactiveRepository.insert(userReportUpload)
      .map(_ => Right(Unit))
      .recover {
        case e: Throwable => Left(Error(e.getMessage, Seq()))
      }
  }

  override def getByReference(reference: String): Future[Either[Error, Option[UserReportUpload]]] = {
    val parsedReference = BSONObjectID.parse(reference)
    if(parsedReference.isFailure) {
      Future.successful(Left(Error(s"$reference could not be parsed as Id", Seq())))
    } else {
      userReportUploadsReactiveRepository.findById(parsedReference.get, ReadPreference.primary)
        .map(Right(_))
        .recover {
          case e: Throwable => Left(Error(e.getMessage, Seq()))
        }
    }
  }
}

@ImplementedBy(classOf[DefaultUserReportUploadsConnector])
trait UserReportUploadsConnector {
  def save(userReportUpload: UserReportUpload): Future[Either[Error, Unit.type]]
  def getByReference(reference: String): Future[Either[Error, Option[UserReportUpload]]]
}
