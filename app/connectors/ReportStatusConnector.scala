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
import models.ReportStatus._
import models.{Error, Login, ReportStatus}
import play.api.mvc.Result
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultReportStatusConnector @Inject() (
  val configuration: Configuration,
  http: HttpClient,
  val serviceConfig: ServicesConfig
)(implicit ec: ExecutionContext
) extends ReportStatusConnector
  with BaseConnector
  with Logging {

  val serviceUrl = s"${serviceConfig.baseUrl("voa-bar")}/voa-bar"

  override def get(login: Login, filter: Option[String] = None)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]] = {
    val filterParam = filter.fold("")(f => s"filter=$f")

    http.GET[Seq[ReportStatus]](s"$serviceUrl/submissions?$filterParam", Seq.empty, defaultHeaders(login.username, login.password))
      .map(Right(_))
      .recover {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
      }
  }

  override def getAll(login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]] =
    http.GET[Seq[ReportStatus]](s"$serviceUrl/submissions/all", Seq.empty, defaultHeaders(login.username, login.password))
      .map(Right(_))
      .recover {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
      }

  override def getByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, ReportStatus]] =
    http.GET[ReportStatus](s"$serviceUrl/submissions/$reference", Seq.empty, defaultHeaders(login.username, login.password))
      .map(Right(_))
      .recover {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
      }

  override def save(reportStatus: ReportStatus, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    http.PUT[ReportStatus, HttpResponse](s"$serviceUrl/submissions?upsert=true", reportStatus, defaultHeaders(login.username, login.password))
      .map(_ => Right(()))
      .recover {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't save submission")))
      }

  override def saveUserInfo(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    http.PUT[ReportStatus, HttpResponse](
      s"$serviceUrl/submissions/user-info",
      ReportStatus(reference, baCode = Some(login.username)),
      defaultHeaders(login.username, login.password)
    )
      .map(_ => Right(()))
      .recover {
        case ex: Throwable =>
          logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't save user info for the submission")))
      }

  override def deleteByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Result, HttpResponse]] = {
    logger.warn(s"Deletion of submission report, reference: $reference, user ${login.username}")

    http.DELETE[HttpResponse](s"$serviceUrl/submissions/$reference", defaultHeaders(login.username, login.password)).map { status =>
      logger.warn(s"Status of deletion for reference ${status.status}, body: ${status.body}")
      Right(status)
    }
  }
}

@ImplementedBy(classOf[DefaultReportStatusConnector])
trait ReportStatusConnector {
  def saveUserInfo(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]]
  def save(reportStatus: ReportStatus, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]]
  def get(login: Login, filter: Option[String] = None)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]]
  def getAll(login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]]
  def getByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, ReportStatus]]
  def deleteByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Result, HttpResponse]]
}
