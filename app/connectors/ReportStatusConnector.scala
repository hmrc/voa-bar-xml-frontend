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
import models.ReportStatus.*
import models.{Error, Login, ReportStatus}
import play.api.Logging
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DefaultReportStatusConnector @Inject() (
  httpClientV2: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext
) extends ReportStatusConnector
  with BaseConnector
  with Logging:

  private val backendBase: String       = servicesConfig.baseUrl("voa-bar")
  private val getAllSubmissionsURL: URL = url"$backendBase/voa-bar/submissions/all"
  private val saveSubmissionURL: URL    = url"$backendBase/voa-bar/submissions?upsert=true"
  private val saveUserInfoURL: URL      = url"$backendBase/voa-bar/submissions/user-info"

  private def getSubmissionsURL(filter: Option[String]): URL =
    val filterParam = filter.fold("")(f => s"filter=$f")
    url"$backendBase/voa-bar/submissions?$filterParam"

  private def submissionByReferenceURL(reference: String): URL =
    url"$backendBase/voa-bar/submissions/$reference"

  private def parseResponse[T](errorMessage: String)(using reads: Reads[T]): HttpResponse => Either[Error, T] =
    response =>
      response.status match {
        case status if is2xx(status) => Right(Json.parse(response.body).as[T])
        case status                  =>
          logger.error(s"$status. $errorMessage. ${response.body}")
          Left(Error("", Seq(s"$status. $errorMessage")))
      }

  private def checkResponseStatus[T](errorMessage: String, mapResponse: HttpResponse => T = identity): HttpResponse => Either[Error, T] =
    response =>
      response.status match {
        case status if is2xx(status) => Right(mapResponse(response))
        case status                  =>
          logger.error(s"$status. $errorMessage. ${response.body}")
          Left(Error("", Seq(s"$status. $errorMessage")))
      }

  private def logAndReturnError[T](errorMessage: String): PartialFunction[Throwable, Either[Error, T]] =
    case ex: Throwable =>
      logger.error(ex.getMessage)
      Left(Error("", Seq(errorMessage)))

  override def get(login: Login, filter: Option[String] = None)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]] =
    httpClientV2.get(getSubmissionsURL(filter))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(parseResponse[Seq[ReportStatus]]("Couldn't get submissions"))
      .recover(logAndReturnError("Couldn't get submissions"))

  override def getAll(login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]] =
    httpClientV2.get(getAllSubmissionsURL)
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(parseResponse[Seq[ReportStatus]]("Couldn't get submissions"))
      .recover(logAndReturnError("Couldn't get submissions"))

  override def getByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, ReportStatus]] =
    httpClientV2.get(submissionByReferenceURL(reference))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(parseResponse[ReportStatus]("Couldn't get submission"))
      .recover(logAndReturnError("Couldn't get submission"))

  override def save(reportStatus: ReportStatus, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    httpClientV2.put(saveSubmissionURL)
      .withBody(Json.toJson(reportStatus))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(checkResponseStatus("Couldn't save submission", _ => ()))
      .recover(logAndReturnError("Couldn't save submission"))

  override def saveUserInfo(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]] =
    httpClientV2.put(saveUserInfoURL)
      .withBody(Json.toJson(ReportStatus(reference, baCode = Some(login.username))))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(checkResponseStatus("Couldn't save user info for the submission", _ => ()))
      .recover(logAndReturnError("Couldn't save user info for the submission"))

  override def deleteByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]] =
    logger.warn(s"Deletion of submission report, reference: $reference, user ${login.username}")

    httpClientV2.delete(submissionByReferenceURL(reference))
      .withBody(Json.toJson(ReportStatus(reference, baCode = Some(login.username))))
      .setHeader(defaultHeaders(login.username, login.password)*)
      .execute[HttpResponse]
      .map(checkResponseStatus(s"Couldn't delete submission for reference $reference"))
      .recover(logAndReturnError(s"Couldn't delete submission for reference $reference"))

@ImplementedBy(classOf[DefaultReportStatusConnector])
trait ReportStatusConnector:
  def saveUserInfo(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]]
  def save(reportStatus: ReportStatus, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Unit]]
  def get(login: Login, filter: Option[String] = None)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]]
  def getAll(login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, Seq[ReportStatus]]]
  def getByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, ReportStatus]]
  def deleteByReference(reference: String, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, HttpResponse]]
