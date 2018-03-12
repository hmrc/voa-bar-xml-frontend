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


import java.io.FileInputStream
import java.nio.file.Paths
import javax.inject.Inject

import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.actions._
import config.FrontendAppConfig
import connectors.DataCacheConnector
import forms.FileUploadDataFormProvider
import identifiers.{CouncilTaxUploadId, VOAAuthorisedId}
import models.{FileUploadData, Mode, NormalMode}
import org.apache.commons.io.IOUtils
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Action, AnyContent}
import play.mvc.BodyParser.MultipartFormData
import views.html.{confirmation, councilTaxUpload}
import utils.{Navigator, UserAnswers}

import scala.concurrent.Future

class CouncilTaxUploadController @Inject()(appConfig: FrontendAppConfig,
                                           override val messagesApi: MessagesApi,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           dataCacheConnector: DataCacheConnector,
                                           formProvider: FileUploadDataFormProvider,
                                           navigator: Navigator) extends FrontendController with I18nSupport {

  val form = formProvider()
  val maxFileSize = 2 * 1024 * 1024

  def onPageLoad = getData.async {
    implicit request =>
      dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString) map {
        case Some(username) => Ok(councilTaxUpload(username, appConfig, form))
        case None => Redirect(routes.LoginController.onPageLoad(NormalMode))
      }
  }

  def onSubmit(mode: Mode, baCode: String) = getData.async(parse.multipartFormData) { implicit request =>
    request.body.file("xml").map { xml =>
      val fileContent = IOUtils.toString(new FileInputStream(xml.ref.file))
      val fileSize = xml.ref.file.length
      val fileName = xml.filename

      fileSize match {
        case a: Long if (a <= 0) => Future.successful(BadRequest(councilTaxUpload(baCode, appConfig, form.withGlobalError("councilTaxUpload.error.xml.required"))))
        case b: Long if (b > 0 && b <= maxFileSize) => {
          if (fileName.endsWith(".xml")) {
            dataCacheConnector.save[FileUploadData](request.externalId, CouncilTaxUploadId.toString, FileUploadData(fileName)) map {
              cm => Redirect(navigator.nextPage(CouncilTaxUploadId, mode)(new UserAnswers(cm)))
            }
          }
          else
            Future.successful(BadRequest(councilTaxUpload(baCode, appConfig, form.withGlobalError("councilTaxUpload.error.xml.fileType"))))
        }
        case c: Long if (c > maxFileSize) => Future.successful(BadRequest(councilTaxUpload(baCode, appConfig, form.withGlobalError("councilTaxUpload.error.xml.length"))))
      }
    }.getOrElse(Future.successful(BadRequest(councilTaxUpload(baCode, appConfig, form.withGlobalError("councilTaxUpload.error.xml.required")))))

  }

}
