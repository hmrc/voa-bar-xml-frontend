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

import javax.inject.Inject
import config.FrontendAppConfig
import connectors.{DataCacheConnector, UploadConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.{CouncilTaxUploadId, LoginId, VOAAuthorisedId}
import models.{Error, FileUploadData, Login, Mode, NormalMode}
import cats.data.EitherT
import cats.implicits._
import models.UpScanRequests._
import models.requests.OptionalDataRequest
import play.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsSuccess, JsValue}
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Action, MultipartFormData, Request, Result}
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
                                           uploadConnector: UploadConnector)
                                          (implicit ec: ExecutionContext)
  extends FrontendController with I18nSupport {

  private[controllers] val form = formProvider()
  private[controllers] val maxFileSize = appConfig.runModeConfiguration.getInt("microservice.services.upscan.max-file-size").get
  private[controllers] val callBackUrl = appConfig.runModeConfiguration.getString("microservice.services.upscan.callback-url").get

  private[controllers] def cachedUserName(externalId: String): Future[Either[Result, String]] = {
    dataCacheConnector.getEntry[String](externalId, VOAAuthorisedId.toString) map {
      case Some(username) => {
        Right(username)
      }
      case None => Left(Redirect(routes.LoginController.onPageLoad(NormalMode)))
    }
  }

  private[controllers] def fileUploadDetails(username: String)
                                            (implicit request: OptionalDataRequest[_]): Future[Either[Result, InitiateResponse]] = {
    uploadConnector.initiate(InitiateRequest(s"$callBackUrl?external-id=${request.externalId}", maxFileSize))
      .map(_.fold(
        _ => Left(BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError("councilTaxUpload.error.fileUploadService")))),
        r => Right(r)
      )
    )
  }

  private[controllers] def loadPage(expectedPage: (String, InitiateResponse) => Result)
                                   (implicit request: OptionalDataRequest[_]): Future[Result] = {
    (for {
      username <- EitherT(cachedUserName(request.externalId))
      initiateResponse <- EitherT(fileUploadDetails(username))
    } yield expectedPage(username, initiateResponse))
      .valueOr(fallBackPage => fallBackPage)
  }

  private[controllers] def badRequest(message: String)(implicit request: OptionalDataRequest[_]): Future[Result] = {
    def badRequestResult(username: String, initiateResponse: InitiateResponse) =
      BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError(message), Some(initiateResponse)))
    loadPage(badRequestResult)
  }

  def onPageLoad = getData.async {
    implicit request => {
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, appConfig, form, Some(initiateResponse)))
      loadPage(okResult)
    }
  }

  private[controllers] def parseUploadConfirmation(request: Request[JsValue], externalId: String): Future[Either[Error, UploadConfirmation]] = {
    Future(request.body.validate[UploadConfirmation] match {
      case uc: JsSuccess[UploadConfirmation] => Right(uc.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        Logger.warn(errorMsg)
        Left(Error("PARSE_ERROR", Seq(errorMsg)))
      }
    })
  }

  private[controllers] def extractExternalId(request: Request[_]) = {
    val queryStringParam = request.queryString.get("external-id")
    val containsExternalId = queryStringParam.isDefined && !queryStringParam.get.isEmpty
    Future(Either.cond(containsExternalId, queryStringParam.get.head, Error("EXT-ID-ND", Seq("External Id not defined."))))
  }

  def onConfirmation = Action.async(parse.tolerantJson) { implicit request =>
    (for {
      externalId <- EitherT(extractExternalId(request))
      uploadConfirmation <- EitherT(parseUploadConfirmation(request, externalId))
      xml <- EitherT(uploadConnector.downloadFile(uploadConfirmation))
      _ <- EitherT(sendContent(externalId, xml, uploadConfirmation))
    } yield NoContent)
      .valueOr(error => {
        val errorMsg = s"Error ${error.code}: ${error.values.mkString("\n")}"
        Logger.error(errorMsg)
        InternalServerError(errorMsg)
      })
  }

  private[controllers] def sendContent(externalId: String, content: String, uploadConfirmation: UploadConfirmation): Future[Either[Error, String]] = {
    val fileUploadData = FileUploadData(content)
    dataCacheConnector.save[FileUploadData](externalId, CouncilTaxUploadId.toString, fileUploadData) flatMap {
      cacheMap =>
        dataCacheConnector.getEntry[Login](externalId, LoginId.toString) flatMap {
          case Some(loginDetails) => {
            uploadConnector.sendXml(content, loginDetails)
          }
          case None => Future(Left(Error("NOAUTH", Seq("Couldn't send file because expired login"))))
        }
    }
  }

  private[controllers] def validateFile(request: OptionalDataRequest[MultipartFormData[Files.TemporaryFile]])
    : Future[Either[Error, Unit.type]] = {
    def validateThereIsAFile: Either[Error, FilePart[TemporaryFile]] = {
      val file = request.body.files.headOption
      Either.cond(file.isDefined && file.get.ref.file.length > 0, file.get, Error("XML_REQUIRED", Seq("councilTaxUpload.error.xml.required")))
    }
    def validateFileIsXml(file: FilePart[TemporaryFile]) =
      Either.cond(file.contentType == Some("text/xml"), file, Error("WRONG_FILE_TYPE", Seq("councilTaxUpload.error.xml.fileType")))
    def validateFileSize(file: FilePart[TemporaryFile]) =
      Either.cond(file.ref.file.length <= maxFileSize, file, Error("FILE_TOO_BIG", Seq("councilTaxUpload.error.xml.length")))
    Future(for {
      file <- validateThereIsAFile
      _ <- validateFileIsXml(file)
      _ <- validateFileSize(file)
    } yield(Unit))
  }

  def onSubmit(mode: Mode) = getData.async(parse.multipartFormData) { implicit request =>
    (for {
      _ <- EitherT(validateFile(request))
      reference <- EitherT(uploadConnector.uploadFile(request))
    } yield Redirect(routes.ConfirmationController.onPageLoad(reference)))
      .valueOrF(error => badRequest(error.values.head))
  }
}
