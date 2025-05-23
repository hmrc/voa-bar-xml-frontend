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

package controllers

import cats.data.EitherT

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import controllers.actions._
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector}
import models.{ConfirmationPayload, Login, Pending, ReportStatus}
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import cats.implicits._
import journey.UniformJourney.{Cr01Cr03Submission, Cr05Submission, CrSubmission}
import play.api.libs.json.JsString
import views.html.components.{confirmation_detail_panel, confirmation_status_panel}

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject() (
  appConfig: FrontendAppConfig,
  override val messagesApi: MessagesApi,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val dataCacheConnector: DataCacheConnector,
  reportStatusConnector: ReportStatusConnector,
  reportConfirmation: views.html.govuk.confirmation,
  confirmation: views.html.confirmation,
  confirmationStatusPanel: confirmation_status_panel,
  confirmationDetailPanel: confirmation_detail_panel,
  val errorTemplate: views.html.error_template,
  controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
  with BaseBarController
  with I18nSupport {

  def onPageLoad(reference: String) = getData.async {
    implicit request =>
      (for {
        login        <- EitherT(cachedLogin(request.externalId))
        reportStatus <- EitherT(getReportStatus(reference, login))
      } yield getCrSubmission(reportStatus) match {
        case None                   => Ok(confirmation(login.username, reference))
        case crSubmission @ Some(_) => Ok(reportConfirmation(login.username, reportStatus, crSubmission))
      }).valueOr(failPage => failPage)
  }

  def onPageRefresh(reference: String) = getData.async {
    implicit request =>
      (for {
        login        <- EitherT(cachedLogin(request.externalId))
        reportStatus <- EitherT(getReportStatus(reference, login))
      } yield getCrSubmission(reportStatus) match {
        case None                   => Ok(confirmation(login.username, reportStatus.id, Some(reportStatus)))
        case crSubmission @ Some(_) => Ok(reportConfirmation(login.username, reportStatus, crSubmission))
      }).valueOr(failPage => failPage)
  }

  def onStatusCheck(reference: String) = getData.async { implicit request =>
    import play.api.libs.json._
    import ConfirmationPayload._

    (for {
      login        <- EitherT(cachedLogin(request.externalId))
      reportStatus <- EitherT(getReportStatus(reference, login))
    } yield {

      // I like this wicked trick, we render same html and send it via ajax and just paste with javascript to the page
      // It's not react, but at least we can be sure that both will render same page.
      val confirmationStatusPanelContent = confirmationStatusPanel(reportStatus.id, Option(reportStatus)).body
      val confirmationDetailPanelContent = confirmationDetailPanel(reportStatus.id, Option(reportStatus)).body

      Ok(Json.toJson(
        ConfirmationPayload(reportStatus.status.getOrElse(Pending.value), confirmationStatusPanelContent, confirmationDetailPanelContent)
      ))
    }).valueOr(failPage => failPage)
  }

  private def getReportStatus(reference: String, login: Login)(implicit request: Request[?]): Future[Either[Result, ReportStatus]] =
    reportStatusConnector.getByReference(reference, login).map(_.fold(
      _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
      reportStatus => Right(reportStatus)
    ))

  private def getCrSubmission(reportStatus: ReportStatus): Option[CrSubmission] =
    reportStatus.report
      .filter(x => x.keys.contains("type"))
      .flatMap { jsObject =>
        jsObject("type") match {
          case JsString("Cr01Cr03Submission") => jsObject.value.get("submission").flatMap(x => Cr01Cr03Submission.format.reads(x).asOpt)
          case JsString("Cr05Submission")     => jsObject.value.get("submission").flatMap(x => Cr05Submission.format.reads(x).asOpt)
          case _                              => None
        }
      }

}
