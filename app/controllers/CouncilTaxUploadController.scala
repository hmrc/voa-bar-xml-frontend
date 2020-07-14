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

import java.time.ZonedDateTime
import java.util.Locale

import javax.inject.Inject
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import models.{Error, Failed, Login, ReportStatus, ReportStatusType, Submitted, UserReportUpload, Verified}
import cats.data.EitherT
import cats.implicits._
import models.UpScanRequests._
import models.requests.OptionalDataRequest
import play.api.{Configuration, Logger}
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.{MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator

import scala.concurrent.{ExecutionContext, Future}

class CouncilTaxUploadController @Inject()(configuration: Configuration,
                                            appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val dataCacheConnector: DataCacheConnector,
                                           formProvider: FileUploadDataFormProvider,
                                           navigator: Navigator,
                                           uploadConnector: UploadConnector,
                                           councilTaxUpload: views.html.councilTaxUpload,
                                           userReportUploadsConnector: UserReportUploadsConnector,
                                           reportStatusConnector: ReportStatusConnector,
                                           controllerComponents: MessagesControllerComponents)
                                          (implicit val ec: ExecutionContext)
  extends FrontendController(controllerComponents) with BaseBarController with I18nSupport {

  val log = Logger(this.getClass)

  implicit val lang: Lang = Lang(Locale.UK)

  private[controllers] val form = formProvider()
  private[controllers] val maxFileSize = configuration
    .get[Int]("microservice.services.upscan.max-file-size")
  private[controllers] val callBackUrl = configuration
    .get[String]("microservice.services.upscan.callback-url")

  private[controllers] def fileUploadDetails(username: String, password: String)
                                            (implicit request: OptionalDataRequest[_]): Future[Either[Result, InitiateResponse]] = {
    val initiateRequest = InitiateRequest(callBackUrl, maxFileSize)
    val errorResult = Left(BadRequest(councilTaxUpload(username, form.withGlobalError(messagesApi("councilTaxUpload.error.fileUploadService")))))
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

  private[controllers] def parseUploadConfirmation(request: Request[JsValue]): Either[Error, UploadConfirmation] = {
    request.body.validate[UploadConfirmation] match {
      case uc: JsSuccess[UploadConfirmation] => Right(uc.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        log.warn(errorMsg)
        Left(Error(messagesApi("councilTaxUpload.error.fileUploadService"), Seq(errorMsg)))
      }
    }
  }

  private[controllers] def parseUploadConfirmationError(request: Request[JsValue]): Either[Error, UploadConfirmationError] = {
    request.body.validate[UploadConfirmationError] match {
      case uc: JsSuccess[UploadConfirmationError] => Right(uc.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        log.warn(errorMsg)
        Left(Error(messagesApi("councilTaxUpload.error.fileUploadService"), Seq(errorMsg)))
      }
    }
  }

  private[controllers] def parseError(request: Request[JsValue]): Either[Result, Error] = {
    request.body.validate[Error] match {
      case error: JsSuccess[Error] => Right(error.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        log.warn(errorMsg)
        Left(InternalServerError(error(messagesApi.preferred(request), appConfig)(request.asInstanceOf[Request[_]])))
      }
    }
  }

  private[controllers] def sendContent(xmlUrl: String, uploadConfirmation: UploadConfirmation, login: Login)(implicit hc: HeaderCarrier): Future[Either[Error, String]] = {
    (for {
      userDataByReference <- EitherT(userReportUploadsConnector.getById(uploadConfirmation.reference, login: Login))
      userData <- EitherT.fromOption[Future](userDataByReference,
        Error(messagesApi("login.error.auth"), Seq("Couldn't send file because of expired submission.")))
      loginDetails = Login(userData.userId, userData.userPassword)
      result <- EitherT(uploadConnector.sendXml(xmlUrl, loginDetails, uploadConfirmation.reference))
    } yield Right(result))
        .valueOr(Left(_))
  }

  def onPageLoad = getData.async {
    implicit request => {
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, form, Some(initiateResponse)))
      loadPage(okResult)
    }
  }

  private def saveReportStatus(
                                uploadConfirmation: UploadConfirmation,
                                login: Login,
                                errors: Seq[Error] = Seq(),
                                status: ReportStatusType = Verified
                              )
                              (implicit request: Request[_]): Future[Either[Error, Unit.type]] = {
    val reportStatus = ReportStatus(
      uploadConfirmation.reference,
      ZonedDateTime.now,
      url = Some(uploadConfirmation.downloadUrl),
      checksum = Some(uploadConfirmation.uploadDetails.checksum),
      status = Some(status.value),
      filename = Some(uploadConfirmation.uploadDetails.fileName),
      errors = Some(errors)
    )
    reportStatusConnector.save(reportStatus, login)
      .map(_.fold(
        _ => Left(Error(s"Couldn't save report status for reference ${uploadConfirmation.reference}", Seq())),
        _ => Right(Unit)
      ))
  }

  private def saveReportStatus(
                                reference: String,
                                login: Login,
                                errors: Seq[Error],
                                status: ReportStatusType
                              )
                              (implicit request: Request[_]): Future[Either[Error, Unit.type]] = {
    val reportStatus = ReportStatus(
      reference,
      ZonedDateTime.now,
      status = Some(status.value),
      errors = Some(errors)
    )
    reportStatusConnector.save(reportStatus, login)
  }

  private def saveReportStatusResult(
    reference: String,
    login: Login,
    errors: Seq[Error],
    status: ReportStatusType
  )(implicit request: Request[_]): Future[Either[Result, Unit.type]] = {
      saveReportStatus(reference, login, errors, status).map(_.fold(
        _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
        _ => Right(Unit)
      ))
  }

  def onConfirmation = getData(parse.tolerantJson) { implicit request =>
    parseUploadConfirmation(request) orElse parseUploadConfirmationError(request) match {
      case Right(u: UploadConfirmation) => {
        onSuccessfulConfirmation(u) //Fire and forget
        NoContent
      }
      case Right(e: UploadConfirmationError) => {
        onFailedConfirmation(e) //Fire and forget
        NoContent
      }
      case _ => InternalServerError("Unable to parse request")
    }
  }

  private def onSuccessfulConfirmation(uploadConfirmation: UploadConfirmation)(implicit request: OptionalDataRequest[JsValue]) = {
    (for {
      login <- EitherT(cachedLoginByReference(uploadConfirmation.reference))
      _ <- EitherT(saveReportStatus(uploadConfirmation, login, status = Verified))          //TODO Confirm verification in db, maybe here we should return OK to upscan and continue in different thread.
      _ <- EitherT(sendContent(uploadConfirmation.downloadUrl, uploadConfirmation, login))  //Send to voa-bar -> eBar
      _ <- EitherT(saveReportStatus(uploadConfirmation, login, status = Submitted))         //Update that everything is ok.
    } yield NoContent)
      .valueOrF(error => {
        handleConfirmationError(request, error)
      })
  }

  private def onFailedConfirmation(uploadConfirmationError: UploadConfirmationError)(implicit request: OptionalDataRequest[JsValue]) = {
    (for {
      login <- EitherT(cachedLoginByReference(uploadConfirmationError.reference))
      _ <- EitherT(saveReportStatus(
          uploadConfirmationError.reference,
          login,
          Seq(Error("4000", Seq(uploadConfirmationError.failureDetails.failureReason))),
          Failed)
        )
    } yield NoContent)
      .valueOr(_ => InternalServerError(error(messagesApi.preferred(request), appConfig)))
  }

  def onError(reference: String) = getData.async(parse.tolerantJson) {
    implicit request =>
      log.warn(s"Unable to upload XML, error detail: ${request.body}")
      (for {
        error <- EitherT(Future.successful(parseError(request)))
        login <- EitherT(cachedLogin(request.externalId))
        _ <- EitherT(saveReportStatusResult(reference, login, Seq(error), Failed))
      } yield NoContent)
        .valueOr(_ => {
          InternalServerError
        })
  }

  private def saveReportStatus(login: Login, reference: String)(implicit request: Request[_]): Future[Either[Result, Unit.type]] = {
    reportStatusConnector.saveUserInfo(reference, login)
      .map(_.fold(
        _ => Left(InternalServerError(error(messagesApi.preferred(request), appConfig))),
        _ => Right(Unit)
      ))
  }

  def onPrepareUpload(reference: String) = getData.async {
    implicit request =>
      (for {
        login <- EitherT(cachedLogin(request.externalId))
        _ <- EitherT(saveLogin(request.externalId, login.copy(reference = Some(reference))))
        _ <- EitherT(saveReportStatus(login, reference))
      } yield NoContent)
        .valueOr(failPage => failPage)
  }

  private def handleConfirmationError(request: OptionalDataRequest[JsValue], error: Error) = {
    val errorMsg = s"Error: code: ${error.code} detail messages: ${error.values.mkString(", ")}"
    log.error(errorMsg)
    (for {
      login <- EitherT(cachedLoginError(request.externalId))
      uploadInfo <- EitherT(Future.successful(parseUploadConfirmation(request)))
      reportStatusError = Error(error.code)
      _ <- EitherT(saveReportStatus(uploadInfo, login, Seq(reportStatusError), Failed)(request))
    } yield InternalServerError(errorMsg))        //TODO maybe we should return 200, because it's message for upscan.
      .valueOr(_ => InternalServerError(errorMsg)) //Here we should report 500, because we have error and can't recover from it.
  }
}
