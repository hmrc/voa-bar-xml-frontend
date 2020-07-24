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

package controllers

import cats.data.EitherT
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.actions._
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector}
import models.{Login, ReportStatus}
import play.api.mvc.{MessagesControllerComponents, Request, Result}

import cats.implicits._
import journey.UniformJourney.Cr03Submission
import play.api.libs.json.JsString

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val dataCacheConnector: DataCacheConnector,
                                       reportStatusConnector: ReportStatusConnector,
                                       reportConfirmation: views.html.govuk.confirmation,
                                       confirmation: views.html.confirmation,
                                       controllerComponents: MessagesControllerComponents)
                                      (implicit val ec: ExecutionContext)
  extends FrontendController(controllerComponents) with BaseBarController with I18nSupport {

  def onPageLoad(reference: String) = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        reportStatus <- EitherT(getReportStatus(reference, login))
      } yield {
        getCr03(reportStatus) match {
          case None =>
            Ok(confirmation(login.username, reference, appConfig))
          case cr03@Some(_) =>
            Ok(reportConfirmation(login.username, reportStatus, cr03))
        }
      }).valueOr(failPage => failPage)
  }

  private def getReportStatus(reference: String, login: Login)(implicit request: Request[_]): Future[Either[Result, ReportStatus]] = {
    reportStatusConnector.getByReference(reference, login).map(_.fold(
      _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
      reportStatus => Right(reportStatus)
    ))
  }

  private def getCr03(reportStatus: ReportStatus): Option[Cr03Submission] = {
    reportStatus.report
      .map(_.value)
      .filter(x => x.get("type").map {case x: JsString => x.value == "Cr03Submission"}.getOrElse(false))
      .flatMap(x => x.get("submission")).flatMap(x => Cr03Submission.format.reads(x).asOpt)
  }

  def onPageRefresh(reference: String) = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        reportStatus <- EitherT(getReportStatus(reference, login))
      } yield {
        getCr03(reportStatus) match {
          case None =>
            Ok(confirmation(login.username, reportStatus.id, appConfig, Some(reportStatus)))
          case cr03@Some(_) =>
            Ok(reportConfirmation(login.username, reportStatus, cr03))
        }
      }).valueOr(failPage => failPage)
  }
}
