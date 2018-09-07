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

import java.time.OffsetDateTime

import javax.inject.Inject
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.LoginId
import models.{Error, Login, NormalMode, ReportStatus, ReportStatusError, ReportStatusType, UpScanVerified, UpscanFailed}
import cats.data.EitherT
import cats.implicits._
import models.UpScanRequests._
import models.requests.OptionalDataRequest
import play.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{Action, Request, Result}
import repositories.UserReportUpload
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator
import views.html.councilTaxUpload

import scala.concurrent.{ExecutionContext, Future}

class CouncilTaxUploadController @Inject()(appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           dataCacheConnector: DataCacheConnector,
                                           formProvider: FileUploadDataFormProvider,
                                           navigator: Navigator,
                                           uploadConnector: UploadConnector,
                                           userReportUploadsConnector: UserReportUploadsConnector,
                                           reportStatusConnector: ReportStatusConnector)
                                          (implicit ec: ExecutionContext)
  extends FrontendController with I18nSupport {

  private[controllers] val form = formProvider()
  private[controllers] val maxFileSize = appConfig.runModeConfiguration
    .getInt("microservice.services.upscan.max-file-size").get
  private[controllers] val callBackUrl = appConfig.runModeConfiguration
    .getString("microservice.services.upscan.callback-url").get

  private[controllers] def cachedLogin(externalId: String): Future[Either[Result, Login]] = {
    dataCacheConnector.getEntry[Login](externalId, LoginId.toString) map {
      case Some(login) => {
        Right(login)
      }
      case None => Left(Redirect(routes.LoginController.onPageLoad(NormalMode)))
    }
  }

  private[controllers] def fileUploadDetails(username: String, password: String)
                                            (implicit request: OptionalDataRequest[_]): Future[Either[Result, InitiateResponse]] = {
    val initiateRequest = InitiateRequest(s"$callBackUrl?external-id=${request.externalId}", maxFileSize)
    val errorResult = Left(BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError("councilTaxUpload.error.fileUploadService"))))
    (for {
      uploadResponse <- EitherT(uploadConnector.initiate(initiateRequest))
      _ <- EitherT(userReportUploadsConnector.save(UserReportUpload(uploadResponse.reference, username, password)))
    } yield Right(uploadResponse))
      .valueOr(_ => errorResult)
  }

  private[controllers] def loadPage(expectedPage: (String, InitiateResponse) => Result)
                                   (implicit request: OptionalDataRequest[_]): Future[Result] = {
    (for {
      login <- EitherT(cachedLogin(request.externalId))
      initiateResponse <- EitherT(fileUploadDetails(login.username, login.password))
    } yield expectedPage(login.username, initiateResponse))
      .valueOr(fallBackPage => fallBackPage)
  }

  private[controllers] def badRequest(message: String)(implicit request: OptionalDataRequest[_]): Future[Result] = {
    def badRequestResult(username: String, initiateResponse: InitiateResponse) =
      BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError(message), Some(initiateResponse)))
    loadPage(badRequestResult)
  }

  private[controllers] def parseUploadConfirmation(request: Request[JsValue]): Future[Either[Error, UploadConfirmation]] = {
    Future.successful(request.body.validate[UploadConfirmation] match {
      case uc: JsSuccess[UploadConfirmation] => Right(uc.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        Logger.warn(errorMsg)
        Left(Error("councilTaxUpload.error.fileUploadService", Seq(errorMsg)))
      }
    })
  }

  private[controllers] def sendContent(content: String, uploadConfirmation: UploadConfirmation): Future[Either[Error, String]] = {
    (for {
      userDataByReference <- EitherT(userReportUploadsConnector.getById(uploadConfirmation.reference))
      userData <- EitherT.fromOption[Future](userDataByReference,
        Error("login.error.auth", Seq("Couldn't send file because of expired submission.")))
      loginDetails = Login(userData.userId, userData.userPassword)
      result <- EitherT(uploadConnector.sendXml(content, loginDetails, uploadConfirmation.reference))
    } yield Right(result))
        .valueOr(Left(_))
  }

  def onPageLoad = getData.async {
    implicit request => {
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, appConfig, form, Some(initiateResponse)))
      loadPage(okResult)
    }
  }

  private def saveReportStatus(
                                uploadConfirmation: UploadConfirmation,
                                errors: Seq[ReportStatusError] = Seq(),
                                status: ReportStatusType = UpScanVerified
                              )
                              (implicit request: Request[_]): Future[Either[Error, Unit.type]] = {
    val reportStatus = ReportStatus(
      uploadConfirmation.reference,
      OffsetDateTime.now,
      url = Some(uploadConfirmation.downloadUrl),
      checksum = Some(uploadConfirmation.uploadDetails.checksum),
      status = Some(status.value),
      errors = Some(errors)
    )
    reportStatusConnector.save(reportStatus)
      .map(_.fold(
        _ => Left(Error(s"Couldn't save report status for reference ${uploadConfirmation.reference}", Seq())),
        _ => Right(Unit)
      ))
  }

  def onConfirmation = Action.async(parse.tolerantJson) { implicit request =>
    (for {
      uploadConfirmation <- EitherT(parseUploadConfirmation(request))
      xml <- EitherT(uploadConnector.downloadFile(uploadConfirmation))
      _ <- EitherT(saveReportStatus(uploadConfirmation))
      _ <- EitherT(sendContent(xml, uploadConfirmation))
    } yield NoContent)
      .valueOrF(error => {
        handleConfirmationError(request, error)
      })
  }

  private def handleConfirmationError(request: Request[JsValue], error: Error) = {
    val errorMsg = s"Error ${error.code}: ${error.values.mkString("\n")}"
    Logger.error(errorMsg)
    (for {
      uploadInfo <- EitherT(parseUploadConfirmation(request))
      reportStatusError = ReportStatusError(error.code, errorMsg, "")
      _ <- EitherT(saveReportStatus(uploadInfo, Seq(reportStatusError), UpscanFailed))
    } yield InternalServerError(errorMsg))
      .valueOr(_ => InternalServerError(errorMsg))
  }
}
