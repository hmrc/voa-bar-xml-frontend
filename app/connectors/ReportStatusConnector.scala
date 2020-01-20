/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.ZonedDateTime

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.{Error, Login, ReportStatus}
import models.ReportStatus._
import play.api.{Configuration, Environment, Logger}
import play.api.Mode.Mode
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultReportStatusConnector @Inject()(
                                      val configuration: Configuration,
                                      http: HttpClient,
                                      environment: Environment,
                                      val serviceConfig: ServicesConfig)
                                     (implicit ec: ExecutionContext)
  extends ReportStatusConnector with BaseConnector {

  val serviceUrl = s"${serviceConfig.baseUrl("voa-bar")}/voa-bar"
  val hc: HeaderCarrier = HeaderCarrier()

  def get(login: Login, filter: Option[String] = None): Future[Either[Error, Seq[ReportStatus]]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers:_*)
    val filterParam = filter.fold("")(f => s"filter=$f")
    http.GET[Seq[ReportStatus]](s"$serviceUrl/submissions?$filterParam")
      .map(Right(_))
      .recover{
        case ex: Throwable => {
          Logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
        }
      }
  }

  def getAll(login: Login): Future[Either[Error, Seq[ReportStatus]]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers:_*)
    http.GET[Seq[ReportStatus]](s"$serviceUrl/submissions/all")
      .map(Right(_))
      .recover{
        case ex: Throwable => {
          Logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
        }
      }
  }

  def getByReference(reference: String, login: Login): Future[Either[Error, ReportStatus]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers:_*)
    http.GET[ReportStatus](s"$serviceUrl/submissions/$reference")
      .map(Right(_))
      .recover{
        case ex: Throwable => {
          Logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't get submissions")))
        }
      }
  }

  def save(reportStatus: ReportStatus, login: Login): Future[Either[Error, Unit.type]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers: _*)
    http.PUT(s"$serviceUrl/submissions?upsert=true", reportStatus)
      .map(_ => Right(Unit))
      .recover {
        case ex: Throwable => {
          Logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't save submission")))
        }
      }
  }

  def saveUserInfo(reference: String, login: Login): Future[Either[Error, Unit.type]] = {
    val headers = defaultHeaders(login.username, login.password)
    implicit val headerCarrier = hc.withExtraHeaders(headers: _*)
    http.PUT(s"$serviceUrl/submissions/user-info", ReportStatus(reference, ZonedDateTime.now, baCode = Some(login.username)))
      .map(_ => Right(Unit))
      .recover {
        case ex: Throwable => {
          Logger.error(ex.getMessage)
          Left(Error("", Seq("Couldn't save user info for the submission")))
        }
      }
  }
}

@ImplementedBy(classOf[DefaultReportStatusConnector])
trait ReportStatusConnector {
  def saveUserInfo(reference: String, login: Login): Future[Either[Error, Unit.type]]
  def save(reportStatus: ReportStatus, login: Login): Future[Either[Error, Unit.type]]
  def get(login: Login, filter: Option[String] = None): Future[Either[Error, Seq[ReportStatus]]]
  def getAll(login: Login): Future[Either[Error, Seq[ReportStatus]]]
  def getByReference(reference: String, login: Login): Future[Either[Error, ReportStatus]]
}
