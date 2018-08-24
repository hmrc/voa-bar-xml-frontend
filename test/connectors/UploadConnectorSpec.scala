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

import java.io.File

import base.SpecBase
import com.typesafe.config.ConfigException
import models.UpScanRequests._
import models._
import org.apache.commons.io.FileUtils
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class UploadConnectorSpec extends SpecBase with MockitoSugar {

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]

  val path = getClass.getResource("/valid.xml")
  val file = new File(path.getPath)
  val tempFile = new TemporaryFile(file)

  val voaBarConfigPath = "microservice.services.voa-bar"
  val voaBarConfig = configuration.getConfig(voaBarConfigPath)
    .getOrElse(throw new ConfigException.Missing(voaBarConfigPath))
  val upScanConfigPath = "microservice.services.upscan"
  val upScanConfig = configuration.getConfig(upScanConfigPath)
    .getOrElse(throw new ConfigException.Missing(upScanConfigPath))
  val upScanCallBackUrlPath = "callback-url"
  val upScanCallBackUrl = upScanConfig.getString(upScanCallBackUrlPath)
    .getOrElse(throw new ConfigException.Missing(upScanCallBackUrlPath))
  val hostPath = "host"
  val portPath = "port"
  val voaBarHost = voaBarConfig.getString(hostPath)
    .getOrElse(throw new ConfigException.Missing(hostPath))
  val voaBarPort = voaBarConfig.getString(portPath)
    .getOrElse(throw new ConfigException.Missing(portPath))
  val maximumFileSizePath = "max-file-size"
  val maximumFileSize = upScanConfig.getInt(maximumFileSizePath)
    .getOrElse(throw new ConfigException.Missing(maximumFileSizePath))
  val upscanHost = upScanConfig.getString(hostPath)
    .getOrElse(throw new ConfigException.Missing(hostPath))
  val upscanPort = upScanConfig.getString(portPath)
    .getOrElse(throw new ConfigException.Missing(voaBarPort))
  val initiateUrlPath = "initiate.url"
  val initiateUrl = upScanConfig.getString(initiateUrlPath)
    .getOrElse(throw new ConfigException.Missing(initiateUrlPath))

  val xmlFile = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)
  val username = "user"
  val password = "pass"
  lazy val login = Login(username, password).encrypt

  val submissionId = "SId3824832"

  def getHttpMock(returnedStatus: Int, returnedString: Option[String]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None, Map(), returnedString))
    httpMock
  }

  "Upload Connector" when {

    "provided with an encrypted Login Input and some xml content" must {

      "call the Microservice with the given xml and login details" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200, Some(submissionId))

        val connector = new UploadConnector(httpMock, configuration, environment)
        val userHeader = connector.generateUsernameHeader(username)
        val passHeader = connector.generatePasswordHeader(login.password)

        await(connector.sendXml(xmlFile, login))

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.baseSegment}upload")
        bodyCaptor.getValue mustBe FileUtils.readFileToByteArray(xmlFile.ref.file)
        headersCaptor.getValue mustBe Seq(connector.xmlContentTypeHeader, userHeader, passHeader)
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        val connector = new UploadConnector(getHttpMock(200, Some(submissionId)), configuration, environment)
        val result = await(connector.sendXml(xmlFile, login))
        result match {
          case Success(submissionValue) => submissionValue mustBe submissionId
          case Failure(e) => assert(false)
        }
      }

      "return a failure representing the error when send method fails" in {
        val connector = new UploadConnector(getHttpMock(500, None), configuration, environment)
        val result = await(connector.sendXml(xmlFile, login))
        assert(result.isFailure)
      }

      "return a failure if the upload call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
          any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)
        val connector = new UploadConnector(httpMock, configuration, environment)
        val result = await(connector.sendXml(xmlFile, login))
        assert(result.isFailure)
      }
    }

    "provided with the proper file restrictions" must {
      "call UpScan initiate endpoint" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[InitiateResponse]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[InitiateRequest]])
        val reference = "11370e18-6e24-453e-b45a-76d3e32ea33d"
        val initiateRequest = InitiateRequest(upScanCallBackUrl, maximumFileSize)
        val uploadUrl = "http://upload.url"
        val initiateResponse = InitiateResponse(
          reference = reference,
          uploadRequest = UploadRequest(
            href = uploadUrl,
            fields = UploadRequestFields(
              `content-type` = "application/xml",
              acl = "private",
              key = "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
              policy = "xxxxxxxx==",
              `x-amz-algorithm` =  "AWS4-HMAC-SHA256",
              `x-amz-credential` =  "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request",
              `x-amz-date` =  "yyyyMMddThhmmssZ",
              `x-amz-meta-callback-url` =  "https://myservice.com/callback",
              `x-amz-signature` =  "xxxx"
            )
          )
        )
        val httpMock = mock[HttpClient]
        when(httpMock.POST[InitiateRequest, InitiateResponse](anyString, any[InitiateRequest], any[Seq[(String, String)]])
          (jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any())) thenReturn Future.successful(initiateResponse)
        val connector = new UploadConnector(httpMock, configuration, environment)

        val response = await(connector.initiate(initiateRequest))

        assert(response.isRight)
        response.right.map(_.reference mustBe reference)
        verify(httpMock, times(1))
          .POST[InitiateRequest, InitiateResponse](anyString, any[InitiateRequest], any[Seq[(String, String)]])(jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any())
      }
    }
  }
}
