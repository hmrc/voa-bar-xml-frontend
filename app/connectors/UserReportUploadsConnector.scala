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

package connectors

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.{Error, Login}
import play.api.Configuration
import models.UserReportUpload
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultUserReportUploadsConnector @Inject() (
  http: HttpClient,
  val configuration: Configuration
)(implicit executionContext: ExecutionContext
) extends UserReportUploadsConnector
  with BaseConnector {

  val voaBarConfig = configuration.get[Configuration]("microservice.services.voa-bar")
  val host         = voaBarConfig.get[String]("host")
  val port         = voaBarConfig.get[String]("port")
  val protocol     = voaBarConfig.get[String]("protocol")
  val serviceUrl   = s"$protocol://$host:$port/voa-bar" // TODO - Refactor with services config

  override def save(userReportUpload: UserReportUpload)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    http.PUT[UserReportUpload, HttpResponse](
      s"$serviceUrl/user-report-upload",
      userReportUpload,
      defaultHeaders(userReportUpload.userId, userReportUpload.userPassword)
    )
      .map(_ => Right(()))
      .recover {
        case e: Throwable => Left(Error(e.getMessage, Seq()))
      }

  override def getById(id: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Option[UserReportUpload]]] = {
    val headers = defaultHeaders(login.username, login.password)

    http.GET[Option[UserReportUpload]](s"$serviceUrl/user-report-upload/$id", Seq.empty, headers)
      .map(Right(_))
      .recover {
        case e: Throwable => Left(Error(e.getMessage, Seq()))
      }
  }
}

@ImplementedBy(classOf[DefaultUserReportUploadsConnector])
trait UserReportUploadsConnector {
  def save(userReportUpload: UserReportUpload)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]]
  def getById(id: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Option[UserReportUpload]]]
}
