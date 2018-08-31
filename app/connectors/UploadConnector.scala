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

package connectors

import akka.stream.scaladsl.{FileIO, Source}
import javax.inject.Inject
import models.UpScanRequests._
import play.api.{Configuration, Environment, Logger}
import play.mvc.Http.Status
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import models.requests.OptionalDataRequest
import models.{Error, Login}
import play.api.Mode.Mode
import play.api.libs.Files
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}

import scala.concurrent.{ExecutionContext, Future}

class UploadConnector @Inject()(http: HttpClient,
                                ws: WSClient,
                                val configuration: Configuration,
                                environment: Environment)
                               (implicit ec: ExecutionContext)
  extends ServicesConfig {

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration

  implicit val hc: HeaderCarrier = HeaderCarrier()
  private[connectors] val serviceUrl = baseUrl("voa-bar")
  private[connectors] val baseSegment = "/voa-bar/"
  private[connectors] val xmlContentTypeHeader = ("Content-Type", "text/plain")
  private[connectors] val upScanConfig = configuration.getConfig("microservice.services.upscan").get
  private[connectors] val upScanPort = upScanConfig.getString("port").get
  private[connectors] val upScanHost = upScanConfig.getString("host").get
  private[connectors] val initiateUrl = s"http://$upScanHost:$upScanPort${upScanConfig.getString("initiate.url").get}"

  def generateUsernameHeader(username: String) = ("BA-Code", username)

  def generatePasswordHeader(password: String) = ("password", password)

  def sendXml(xml: String, loginDetails: Login): Future[Either[Error, String]] = {
    val baCode = loginDetails.username
    val password = loginDetails.password
    http.POST(s"$serviceUrl${baseSegment}upload", xml, Seq(xmlContentTypeHeader, generateUsernameHeader(baCode), generatePasswordHeader(password)))
      .map {
        response =>
          response.status match {
            case Status.OK => Right(response.body)
            case status => {
              val errorMsg = s"Received status of $status from upstream service when uploading am xml file"
              Logger.warn(errorMsg)
              Left(Error("councilTaxUpload.error.fileUploadService", Seq(errorMsg)))
            }
          }
      } recover {
      case e =>
        val errorMsg = s"Received status of ${e.getMessage} from upstream service when uploading am xml file"
        Logger.error(errorMsg, e)
        Left(Error("councilTaxUpload.error.fileUploadService", Seq(errorMsg)))
    }
  }

  def initiate(request: InitiateRequest): Future[Either[Error, InitiateResponse]] = {
    http.POST[InitiateRequest, InitiateResponse](initiateUrl, request)
      .map(Right(_))
      .recover {
        case ex: Throwable => {
          val errorMessage = "Failed to get UpScan file upload details"
          Logger.error(errorMessage, ex)
          Left(Error("councilTaxUpload.error.fileUploadService", Seq(errorMessage)))
        }
      }
  }

  def downloadFile(request: UploadConfirmation): Future[Either[Error, String]] = {
    http.doGet(request.downloadUrl)
      .map{ response =>
        Right(response.body)
      }
      .recover {
        case ex: Throwable => {
          val errorMsg = s"Error downloading file from ${request.downloadUrl}"
          Logger.error(errorMsg, ex)
          Left(Error("councilTaxUpload.error.fileUploadService", Seq(ex.getMessage)))
        }
      }
  }

  private val validStatuses = Seq(Status.OK, Status.NO_CONTENT)
  def uploadFile(request: OptionalDataRequest[MultipartFormData[Files.TemporaryFile]]): Future[Either[Error, String]] = {
      val file = request.body.files.head
      val uploadUrl = request.body.dataParts.get("uploadUrl").get.head
      val reference = request.body.dataParts.get("reference").get.head
      val filePart = FilePart(file.key, file.filename, file.contentType, FileIO.fromPath(file.ref.file.toPath))
      val dataParts = request.body.dataParts.map{case (key, value) => DataPart(key, value.head)}
      val parts = filePart :: dataParts.toList
      ws.url(uploadUrl)
        .post(Source(parts))
      .map (response =>
        if (validStatuses.contains(response.status)) {
          Right(reference)
        } else {
          val file = request.body.files.head
          val errorMsg = s"Error Uploading file ${file.filename}"
          Logger.warn(s"$errorMsg\n${response.body}")
          Left(Error("councilTaxUpload.error.fileUploadService", Seq()))
        }
      ).recover{
        case ex: Throwable => {
          val errorMsg = "Error while uploading to upscan."
          Logger.error(errorMsg, ex)
          Left(Error("councilTaxUpload.error.fileUploadService", Seq()))
        }
      }

  }
}
