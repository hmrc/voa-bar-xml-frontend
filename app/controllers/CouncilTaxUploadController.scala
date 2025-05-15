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
import cats.implicits.*
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions.*
import forms.FileUploadDataFormProvider
import models.UpScanRequests.*
import models.requests.OptionalDataRequest
import models.{Error, Failed, Login, ReportStatus, ReportStatusType, UserReportUpload}
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.*
import play.api.{Configuration, Logger}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Navigator

import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CouncilTaxUploadController @Inject() (
  configuration: Configuration,
  appConfig: FrontendAppConfig,
  override val messagesApi: MessagesApi,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val dataCacheConnector: DataCacheConnector,
  formProvider: FileUploadDataFormProvider,
  navigator: Navigator,
  uploadConnector: UploadConnector,
  councilTaxUpload: views.html.councilTaxUpload,
  val errorTemplate: views.html.error_template,
  userReportUploadsConnector: UserReportUploadsConnector,
  reportStatusConnector: ReportStatusConnector,
  controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext
) extends FrontendController(controllerComponents)
  with BaseBarController
  with I18nSupport {

  val log = Logger(this.getClass)

  implicit val lang: Lang = Lang(Locale.UK)

  private[controllers] val form = formProvider()

  private[controllers] val maxFileSize = configuration
    .get[Int]("microservice.services.upscan.max-file-size")

  private[controllers] val callBackUrl = configuration
    .get[String]("microservice.services.upscan.callback-url")

  private[controllers] def fileUploadDetails(username: String, password: String)(implicit request: OptionalDataRequest[?])
    : Future[Either[Result, InitiateResponse]] = {
    val initiateRequest = InitiateRequest(s"$callBackUrl/$username", maxFileSize)
    val errorResult     = Left(BadRequest(councilTaxUpload(username, form.withGlobalError(messagesApi("councilTaxUpload.error.fileUploadService")))))
    (for {
      uploadResponse <- EitherT(uploadConnector.initiate(initiateRequest))
      _              <- EitherT(userReportUploadsConnector.save(UserReportUpload(uploadResponse.reference, username, password)))
    } yield Right(uploadResponse))
      .valueOr(_ => errorResult)
  }

  private[controllers] def loadPage(expectedPage: (String, InitiateResponse) => Result)(implicit request: OptionalDataRequest[?]): Future[Result] =
    (for {
      login            <- EitherT(cachedLogin(request.externalId))
      initiateResponse <- EitherT(fileUploadDetails(login.username, login.password))
    } yield expectedPage(login.username, initiateResponse))
      .valueOr(fallBackPage => fallBackPage)

  private[controllers] def parseError(request: Request[JsValue]): Either[Result, Error] =
    request.body.validate[Error] match {
      case JsSuccess(error, _) => Right(error)
      case _                   =>
        val errorMsg = s"Couldn't parse: \n${request.body}"
        log.warn(errorMsg)
        Left(InternalServerError(error(messagesApi.preferred(request), appConfig)(using request.asInstanceOf[Request[?]])))
    }

  def onPageLoad(showEmptyError: Boolean): Action[AnyContent] = getData.async {
    implicit request =>
      val formWithError                                                  =
        if (showEmptyError) {
          form
            .withError("file", messagesApi("error.file.empty"))
        } else {
          form
        }
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, formWithError, Some(initiateResponse)))
      loadPage(okResult)
  }

  private def saveReportStatus(
    reference: String,
    login: Login,
    errors: Seq[Error],
    status: ReportStatusType
  )(implicit request: Request[?]
  ): Future[Either[Error, Unit]] = {
    val reportStatus = ReportStatus(
      reference,
      status = Some(status.value),
      errors = errors
    )
    reportStatusConnector.save(reportStatus, login)
  }

  private def saveReportStatusResult(
    reference: String,
    login: Login,
    errors: Seq[Error],
    status: ReportStatusType
  )(implicit request: Request[?]
  ): Future[Either[Result, Unit]] =
    saveReportStatus(reference, login, errors, status).map(_.fold(
      _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
      _ => Right(())
    ))

  def onError(reference: String): Action[JsValue] = getData.async(parse.tolerantJson) {
    implicit request =>
      log.warn(s"Unable to upload XML, error detail: ${request.body}")
      (for {
        error <- EitherT(Future.successful(parseError(request)))
        login <- EitherT(cachedLogin(request.externalId))
        _     <- EitherT(saveReportStatusResult(reference, login, Seq(error), Failed))
      } yield NoContent)
        .valueOr { _ =>
          InternalServerError
        }
  }

  private def saveReportStatus(login: Login, reference: String)(implicit request: Request[?]): Future[Either[Result, Unit]] =
    reportStatusConnector.saveUserInfo(reference, login)
      .map(_.fold(
        _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
        _ => Right(())
      ))

  def onPrepareUpload(reference: String): Action[AnyContent] = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        _     <- EitherT(saveLogin(request.externalId, login.copy(reference = Some(reference))))
        _     <- EitherT(saveReportStatus(login, reference))
      } yield NoContent)
        .valueOr(failPage => failPage)
  }

}
