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

package controllers

import java.time.format.DateTimeFormatter

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector}
import controllers.actions._
import models.{Login, ReportStatus}
import play.api.http.HeaderNames
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.Result
import services.ReceiptService
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.reportStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ReportStatusController @Inject()(appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       val dataCacheConnector: DataCacheConnector,
                                       reportStatusConnector: ReportStatusConnector,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       receiptService: ReceiptService
                                      )(implicit val ec: ExecutionContext)
  extends FrontendController with BaseBarController with I18nSupport {
  def verifyResponse(json: JsValue): Either[String, Seq[ReportStatus]] = {
    val reportStatuses = json.asOpt[Seq[ReportStatus]]
    reportStatuses match {
      case Some(response) => Right(response)
      case None => Left("Unable to parse the response from the Report Status Connector")
    }
  }

  private def reportStatuses(login: Login): Future[Either[Result, Seq[ReportStatus]]] = {
    reportStatusConnector.get(login).map(_.fold(
      _ => Left(InternalServerError),
      reportStatuses => Right(reportStatuses)
    ))
  }

  private def getReportStatus(submissionId: String, login: Login): Future[Either[Result, ReportStatus]] = {
    reportStatusConnector.getByReference(submissionId, login).map(_.fold(
      _ => Left(InternalServerError),
      reportStatus => Right(reportStatus)
    ))
  }

  def onPageLoad() = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        reportStatuses <- EitherT(reportStatuses(login))
      } yield Ok(reportStatus(login.username, appConfig, reportStatuses)))
        .valueOr(f => f)
  }

  private def getPDF(reportStatus: ReportStatus): Future[Either[Result, Array[Byte]]] = {
    Future{receiptService.producePDF(reportStatus) match {
        case Success(content) => Right(content)
        case Failure(exception) => Left(InternalServerError)
      }
    }
  }

  def onReceiptDownload(submissionId: String) = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        reportStatus <- EitherT(getReportStatus(submissionId, login))
        data <- EitherT(getPDF(reportStatus))
        date = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(reportStatus.created)
      } yield Ok(data).withHeaders(
          HeaderNames.CONTENT_TYPE -> withCharset("application/pdf"),
          HeaderNames.CONTENT_DISPOSITION -> s"""attachment; filename=${reportStatus.filename.getOrElse("Submission")}_Report-${reportStatus.baCode.getOrElse("").toUpperCase}-$date.pdf"""
        ))
        .valueOr(f => f)
  }
}
