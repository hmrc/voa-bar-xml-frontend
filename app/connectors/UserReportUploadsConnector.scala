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

import com.google.inject.ImplementedBy
import models.{Error, Login, UserReportUpload}
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.{URI, URL}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class DefaultUserReportUploadsConnector @Inject() (
  httpClientV2: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext
) extends UserReportUploadsConnector
  with BaseConnector:

  private val backendBase: String      = servicesConfig.baseUrl("voa-bar")
  private val userReportUploadURL: URL = url"$backendBase/voa-bar/user-report-upload"
  private val userReportUploadURI: URI = userReportUploadURL.toURI

  private def getUserReportUploadURL(id: String): URL = userReportUploadURI.resolve(s"/$id").toURL

  override def save(userReportUpload: UserReportUpload)(using hc: HeaderCarrier): Future[Either[Error, Unit]] =
    httpClientV2.put(userReportUploadURL)
      .setHeader(defaultHeaders(userReportUpload.userId, userReportUpload.userPassword)*)
      .withBody(Json.toJson(userReportUpload))
      .execute[HttpResponse]
      .map { r =>
        r.status match {
          case status if is2xx(status) => Right(())
          case status                  => Left(Error(s"$status. Couldn't save UserReportUpload"))
        }
      }
      .recover {
        case e: Throwable => Left(Error(e.getMessage))
      }

  override def getById(id: String, login: Login)(using hc: HeaderCarrier): Future[Either[Error, Option[UserReportUpload]]] =
    httpClientV2.get(getUserReportUploadURL(id))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map { r =>
        r.status match {
          case status if is2xx(status) => Right(Try(Json.parse(r.body).asOpt[UserReportUpload]).getOrElse(None))
          case status                  => Left(Error(s"$status. Couldn't get UserReportUpload"))
        }
      }
      .recover {
        case e: Throwable => Left(Error(e.getMessage))
      }

@ImplementedBy(classOf[DefaultUserReportUploadsConnector])
trait UserReportUploadsConnector:
  def save(userReportUpload: UserReportUpload)(using hc: HeaderCarrier): Future[Either[Error, Unit]]
  def getById(id: String, login: Login)(using hc: HeaderCarrier): Future[Either[Error, Option[UserReportUpload]]]
