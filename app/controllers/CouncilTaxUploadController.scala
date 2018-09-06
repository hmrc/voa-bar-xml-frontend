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
import connectors.{DataCacheConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.{CouncilTaxUploadId, LoginId}
import models.{Error, FileUploadData, Login, NormalMode}
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
                                           userReportUploadsConnector: UserReportUploadsConnector)
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

  private[controllers] def parseUploadConfirmation(request: Request[JsValue], externalId: String): Future[Either[Error, UploadConfirmation]] = {
    Future(request.body.validate[UploadConfirmation] match {
      case uc: JsSuccess[UploadConfirmation] => Right(uc.get)
      case _ => {
        val errorMsg = s"Couldn't parse: \n${request.body}"
        Logger.warn(errorMsg)
        Left(Error("councilTaxUpload.error.fileUploadService", Seq(errorMsg)))
      }
    })
  }

  private[controllers] def extractExternalId(request: Request[_]) = {
    val queryStringParam = request.queryString.get("external-id")
    val containsExternalId = queryStringParam.isDefined && !queryStringParam.get.isEmpty
    Future(Either.cond(containsExternalId, queryStringParam.get.head, Error("councilTaxUpload.error.fileUploadService", Seq("External Id not defined."))))
  }

  private[controllers] def sendContent(externalId: String, content: String, uploadConfirmation: UploadConfirmation): Future[Either[Error, String]] = {
    val fileUploadData = FileUploadData(content)
    dataCacheConnector.save[FileUploadData](externalId, CouncilTaxUploadId.toString, fileUploadData) flatMap {
      cacheMap =>
        dataCacheConnector.getEntry[Login](externalId, LoginId.toString) flatMap {
          case Some(loginDetails) => {
            uploadConnector.sendXml(content, loginDetails, uploadConfirmation.reference)
          }
          case None => Future(Left(Error("login.error.auth", Seq("Couldn't send file because expired login"))))
        }
    }
  }

  def onPageLoad = getData.async {
    implicit request => {
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, appConfig, form, Some(initiateResponse)))
      loadPage(okResult)
    }
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
}
