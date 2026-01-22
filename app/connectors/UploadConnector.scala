/*
 * Copyright 2026 HM Revenue & Customs
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

import models.UpScanRequests.*
import models.{Error, Login, VoaBarUpload}
import play.api.Logging
import play.api.http.Status.OK
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadConnector @Inject() (
  httpClientV2: HttpClientV2,
  val servicesConfig: ServicesConfig,
  messages: MessagesApi
)(implicit ec: ExecutionContext
) extends BaseConnector
  with Logging:

  private val backendBase: String = servicesConfig.baseUrl("voa-bar")
  private val uploadURL: URL      = url"$backendBase/voa-bar/upload"

  private val upscanBase: String     = servicesConfig.baseUrl("upscan")
  private val initiateUrl: String    = s"$upscanBase${servicesConfig.getConfString("upscan.initiate.url", "")}"
  private val upscanInitiateURL: URL = url"$initiateUrl"

  given Lang = Lang(Locale.UK)

  private def handleSendXmlError(message: String): Either[Error, String] =
    val errorMsg = s"Error when uploading am xml file\n$message"
    logger.warn(errorMsg)
    Left(Error(messages("councilTaxUpload.error.transferXml"), Seq(messages("status.failed.description"))))

  def sendXml(xmlUrl: String, login: Login, id: String)(using hc: HeaderCarrier): Future[Either[Error, String]] =
    val uploadData = VoaBarUpload(id, xmlUrl)

    httpClientV2.post(uploadURL)
      .setHeader(defaultHeaders(login.username, login.password)*)
      .withBody(Json.toJson(uploadData))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK     => Right(response.body)
          case status => handleSendXmlError(s"$status. ${response.body}")
        }
      }
      .recover {
        case e => handleSendXmlError(e.getMessage)
      }

  def initiate(request: InitiateRequest)(using hc: HeaderCarrier): Future[Either[Error, InitiateResponse]] =
    httpClientV2.post(upscanInitiateURL)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case status if is2xx(status) =>
            val initiateResponse = Json.parse(response.body).as[InitiateResponse]
            logger.debug(s"Upscan initiate response : $initiateResponse")
            Right(initiateResponse)
          case status                  =>
            logger.error(s"$status. Failed to get UpScan file upload details. ${response.body}")
            Left(Error(messages("councilTaxUpload.error.fileUploadService"), Seq()))
        }
      }
      .recover {
        case ex =>
          logger.error("Failed to get UpScan file upload details", ex)
          Left(Error(messages("councilTaxUpload.error.fileUploadService"), Seq()))
      }
