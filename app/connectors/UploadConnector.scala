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

import javax.inject.Inject
import models.UpScanRequests._
import models.{Error, Login}
import org.apache.commons.io.FileUtils
import play.api.Mode.Mode
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class UploadConnector @Inject()(http: HttpClient,
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

  def sendXml(xmlFile: FilePart[TemporaryFile], loginDetails: Login): Future[Try[String]] = {
    val baCode = loginDetails.username
    val password = loginDetails.password
    http.POST(s"$serviceUrl${baseSegment}upload", FileUtils.readFileToByteArray(xmlFile.ref.file), Seq(xmlContentTypeHeader, generateUsernameHeader(baCode), generatePasswordHeader(password)))
      .map {
        response =>
          response.status match {
            case 200 => Success(response.body)
            case status => {
              Logger.warn("Received status of " + status + " from upstream service when uploading am xml file")
              Failure(new RuntimeException("Received status of " + status + " from upstream service when uploading an xml file"))
            }
          }
      } recover {
      case e =>
        Logger.warn("Received exception " + e.getMessage + " from upstream service when uploading am xml file")
        Failure(new RuntimeException("Received exception " + e.getMessage + " from upstream service when uploading am xml file"))
    }
  }

  def initiate(request: InitiateRequest): Future[Either[Error, InitiateResponse]] = {
    http.POST[InitiateRequest, InitiateResponse](initiateUrl, request)
      .map(Right(_))
      .recover {
        case ex: Throwable => {
          val errorMessage = "Failed to get UpScan file upload details"
          Logger.error(errorMessage, ex)
          Left(Error("UPSCAN-INIT", Seq(errorMessage)))
        }
      }
  }

}
