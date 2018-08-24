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
import models.{FileUploadData, Login, Mode, NormalMode}
import cats.data.EitherT
import cats.implicits._
import models.UpScanRequests.{InitiateRequest, InitiateResponse}
import models.requests.OptionalDataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator
import views.html.councilTaxUpload

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


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
                                            (implicit request: Request[_]): Future[Either[Result, InitiateResponse]] = {
    EitherT(uploadConnector.initiate(InitiateRequest(callBackUrl, maxFileSize)))
      .fold(
        _ => Left(BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError("councilTaxUpload.error.fileUploadService")))),
        r => Right(r)
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
      BadRequest(councilTaxUpload(username, appConfig, form.withGlobalError(message)))
    loadPage(badRequestResult)
  }

  def onPageLoad = getData.async {
    implicit request => {
      def okResult(username: String, initiateResponse: InitiateResponse) =
        Ok(councilTaxUpload(username, appConfig, form, Some(initiateResponse)))
      loadPage(okResult)
    }
  }

  def onSubmit(mode: Mode) = getData.async(parse.multipartFormData) { implicit request =>
    request.body.file("xml").map { xmlFile =>
      val fileSize = xmlFile.ref.file.length
      val fileName = xmlFile.filename

      fileSize match {
        case a: Long if a <= 0 => badRequest("councilTaxUpload.error.xml.required")
        case b: Long if b <= maxFileSize => {
          if (fileName.endsWith(".xml")) {
            dataCacheConnector.save[FileUploadData](request.externalId, CouncilTaxUploadId.toString, FileUploadData(fileName)) flatMap {
              cacheMap =>
                dataCacheConnector.getEntry[Login](request.externalId, LoginId.toString) flatMap {
                  case Some(loginDetails) => {
                    uploadConnector.sendXml(xmlFile, loginDetails) map {
                      case Success(submissionId) => Redirect(routes.ConfirmationController.onPageLoad(submissionId))
                      case Failure(ex) => throw new RuntimeException("Uploading xml file failed with message: " + ex.getMessage)
                    }
                  }
                  case None => Future.successful(Redirect(routes.LoginController.onPageLoad(NormalMode)))
                }
            }
          }
          else
            badRequest(("councilTaxUpload.error.xml.fileType"))
        }
        case _ => badRequest("councilTaxUpload.error.xml.length")
      }
    }
      .getOrElse(badRequest("councilTaxUpload.error.xml.required"))

  }

}
