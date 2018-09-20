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
import models.{Error, Login}
import play.api.Configuration
import repositories.UserReportUpload
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultUserReportUploadsConnector @Inject() (
                                                    http: HttpClient,
                                                    val configuration: Configuration
                                                   )(implicit executionContext: ExecutionContext)
  extends  UserReportUploadsConnector with BaseConnector {
  val hc: HeaderCarrier = HeaderCarrier()
  val voaBarConfig = configuration.getConfig("microservice.services.voa-bar").get
  val host = voaBarConfig.getString("host").get
  val port = voaBarConfig.getString("port").get
  val protocol = voaBarConfig.getString("protocol").get
  val serviceUrl = s"$protocol://$host:$port/voa-bar"
  override def save(userReportUpload: UserReportUpload): Future[Either[Error, Unit.type]] = {
    val headers = defaultHeaders(userReportUpload.userId, userReportUpload.userPassword)
    implicit val headerCarrier = hc.withExtraHeaders(headers:_*)
    http.PUT(s"$serviceUrl/user-report-upload", userReportUpload)
      .map(_ => Right(Unit))
      .recover {
        case e: Throwable => Left(Error(e.getMessage, Seq()))
      }
  }

  override def getById(id: String, login: Login): Future[Either[Error, Option[UserReportUpload]]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers:_*)
    http.GET[Option[UserReportUpload]](s"$serviceUrl/user-report-upload/$id")
      .map(Right(_))
      .recover {
        case e: Throwable => Left(Error(e.getMessage, Seq()))
      }
  }
}

@ImplementedBy(classOf[DefaultUserReportUploadsConnector])
trait UserReportUploadsConnector {
  def save(userReportUpload: UserReportUpload): Future[Either[Error, Unit.type]]
  def getById(id: String, login: Login): Future[Either[Error, Option[UserReportUpload]]]
}
