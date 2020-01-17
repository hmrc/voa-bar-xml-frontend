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

package connectors

import java.util.Locale

import javax.inject.{Inject, Singleton}
import models.UpScanRequests._
import play.api.{Configuration, Environment, Logger}
import play.mvc.Http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import models.{Error, Login, VoaBarUpload}
import play.api.Mode.Mode
import play.api.i18n.{Lang, MessagesApi}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadConnector @Inject()(http: HttpClient,
                                val configuration: Configuration,
                                val servicesConfig: ServicesConfig,
                                messages: MessagesApi)
                               (implicit ec: ExecutionContext) {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private[connectors] val serviceUrl = servicesConfig.baseUrl("voa-bar")
  private[connectors] val baseSegment = "/voa-bar/"
  private[connectors] val xmlContentTypeHeader = ("Content-Type", "text/plain")
  private[connectors] val upScanConfig = configuration.getConfig("microservice.services.upscan").get
  private[connectors] val upScanPort = upScanConfig.getString("port").get
  private[connectors] val upScanHost = upScanConfig.getString("host").get
  private[connectors] val upScanProtocol = upScanConfig.getString("protocol").get
  private[connectors] val initiateUrl = s"$upScanProtocol://$upScanHost:$upScanPort${upScanConfig.getString("initiate.url").get}"

  def generateUsernameHeader(username: String) = ("BA-Code", username)

  def generatePasswordHeader(password: String) = ("password", password)

  def sendXml(xmlUrl: String, loginDetails: Login, id: String): Future[Either[Error, String]] = {
    val baCode = loginDetails.username
    val password = loginDetails.password
    val headers = Seq(generateUsernameHeader(baCode), generatePasswordHeader(password))

    val uploadData = VoaBarUpload(id, xmlUrl)

    http.POST(s"$serviceUrl${baseSegment}upload", uploadData, headers)
      .map {
        response =>
          response.status match {
            case Status.OK => Right(response.body)
            case status => {
              handleSendXmlError(response.body)
            }
          }
      } recover {
      case e =>
        handleSendXmlError(e.getMessage)
    }
  }

  implicit val lang: Lang = Lang(Locale.UK)

  private def handleSendXmlError(message: String) = {
    val errorMsg = s"Error when uploading am xml file\n$message"
    Logger.warn(errorMsg)
    Left(Error(messages("councilTaxUpload.error.transferXml"), Seq(messages("status.failed.description"))))
  }

  def initiate(request: InitiateRequest): Future[Either[Error, InitiateResponse]] = {
    http.POST[InitiateRequest, InitiateResponse](initiateUrl, request)
      .map(Right(_))
      .recover {
        case ex: Throwable => {
          val errorMessage = "Failed to get UpScan file upload details"
          Logger.error(errorMessage, ex)
          Left(Error(messages("councilTaxUpload.error.fileUploadService"), Seq()))
        }
      }
  }

}
