/*
 * Copyright 2019 HM Revenue & Customs
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

import java.io.{File, PrintWriter}
import java.time.OffsetDateTime

import base.SpecBase
import com.typesafe.config.ConfigException
import models.UpScanRequests.{UploadDetails, _}
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class UploadConnectorSpec extends SpecBase with MockitoSugar {

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]

  val file = File.createTempFile("foo", "bar")
  val path = new PrintWriter(file) { write("<xml />"); close }
  val tempFile = new TemporaryFile(file)

  val upScanConfigPath = "microservice.services.upscan"
  val upScanConfig = configuration.getConfig(upScanConfigPath)
    .getOrElse(throw new ConfigException.Missing(upScanConfigPath))
  val upScanCallBackUrlPath = "callback-url"
  val upScanCallBackUrl = upScanConfig.getString(upScanCallBackUrlPath)
    .getOrElse(throw new ConfigException.Missing(upScanCallBackUrlPath))
  val maximumFileSizePath = "max-file-size"
  val maximumFileSize = upScanConfig.getInt(maximumFileSizePath)
    .getOrElse(throw new ConfigException.Missing(maximumFileSizePath))

  val xmlFile = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)
  val xmlContent = Source.fromFile(file).getLines.mkString("\n")
  val username = "user"
  val password = "pass"
  lazy val login = Login(username, password).encrypt

  val submissionId = "SId3824832"

  def getHttpMock(returnedStatus: Int, returnedString: Option[String]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None, Map(), returnedString))
    when(httpMock.POSTString(anyString, any[String], any[Seq[(String, String)]])(any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None, Map(), returnedString))
    httpMock
  }

  "Upload Connector" when {

    "provided with an encrypted Login Input and some xml content" must {

      "call the Microservice with the given xml and login details" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[Any]])
        val messagesApi = injector.instanceOf[MessagesApi]
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[String])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(Status.OK, Some(submissionId))

        val connector = new UploadConnector(httpMock, configuration, environment, messagesApi)
        val userHeader = connector.generateUsernameHeader(username)
        val passHeader = connector.generatePasswordHeader(login.password)

        await(connector.sendXml(xmlContent, login, submissionId))

        verify(httpMock)
          .POSTString(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.baseSegment}upload?reference=$submissionId")
        bodyCaptor.getValue mustBe Source.fromFile(xmlFile.ref.file).getLines.mkString("\n")
        headersCaptor.getValue mustBe Seq(connector.xmlContentTypeHeader, userHeader, passHeader)
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        val connector = new UploadConnector(getHttpMock(Status.OK, Some(submissionId)), configuration, environment, messagesApi)
        val result = await(connector.sendXml(xmlContent, login, submissionId))
        result match {
          case Right(submissionValue) => submissionValue mustBe submissionId
          case Left(e) => assert(false)
        }
      }

      "return a failure representing the error when send method fails" in {
        val connector = new UploadConnector(getHttpMock(Status.INTERNAL_SERVER_ERROR, None), configuration, environment, messagesApi)
        val result = await(connector.sendXml(xmlContent, login, submissionId))
        assert(result.isLeft)
      }

      "return a failure if the upload call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POSTString(anyString, any[String], any[Seq[(String, String)]])(any[HttpReads[Any]],
          any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)
        val connector = new UploadConnector(httpMock, configuration, environment, messagesApi)
        val result = await(connector.sendXml(xmlContent, login, submissionId))
        assert(result.isLeft)
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
            fields = Map(
              ("acl", "private"),
              ("key", "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
              ("policy", "xxxxxxxx=="),
              ("x-amz-algorithm", "AWS4-HMAC-SHA256"),
              ("x-amz-credential", "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request"),
              ("x-amz-date", "yyyyMMddThhmmssZ"),
              ("x-amz-meta-callback-url", "https://myservice.com/callback"),
              ("x-amz-signature", "xxxx"),
              ("x-amz-meta-consuming-service", "something"),
              ("x-amz-meta-session-id", "session-1234567890"),
              ("x-amz-meta-request-id", "request-12345789")
            )
          )
        )
        val httpMock = mock[HttpClient]
        when(httpMock.POST[InitiateRequest, InitiateResponse](anyString, any[InitiateRequest], any[Seq[(String, String)]])
          (jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any())) thenReturn Future.successful(initiateResponse)
        val connector = new UploadConnector(httpMock, configuration, environment, messagesApi)

        val response = await(connector.initiate(initiateRequest))

        assert(response.isRight)
        response.right.map(_.reference mustBe reference)
        verify(httpMock, times(1))
          .POST[InitiateRequest, InitiateResponse](anyString, any[InitiateRequest], any[Seq[(String, String)]])(jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any())
      }
    }

    "provided with upscan information" must {
      val reference = "11370e18-6e24-453e-b45a-76d3e32ea33d"
      val downloadUrl = "http://download.url"
      val downloadRequest = UploadConfirmation (
        reference = reference,
        downloadUrl = downloadUrl,
        fileStatus = "fine",
        uploadDetails = UploadDetails(
          uploadTimestamp = OffsetDateTime.now,
          checksum = "checksum",
          fileMimeType = "text/plain",
          fileName = "filename.xml"))
      "download a file succesfully" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        val content = "<foo/>"
        val httpResponse = mock[HttpResponse]
        when(httpResponse.body) thenReturn(content)
        val httpMock = mock[HttpClient]
        when(httpMock.doGet(any[String])(headerCarrierNapper.capture)) thenReturn Future.successful(httpResponse)
        val connector = new UploadConnector(httpMock, configuration, environment, messagesApi)

        val response = await(connector.downloadFile(downloadRequest))

        assert(response.isRight)
        response.right.map(_ mustBe content)
        verify(httpMock, times(1))
          .doGet(any[String])(headerCarrierNapper.capture)
      }
      "fail when attmpting to download a file when there is a problem with upscan" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        val httpMock = mock[HttpClient]
        when(httpMock.doGet(any[String])(headerCarrierNapper.capture)) thenReturn Future.failed(new Exception())
        val connector = new UploadConnector(httpMock, configuration, environment, messagesApi)

        val response = await(connector.downloadFile(downloadRequest))

        assert(response.isLeft)
        verify(httpMock, times(1))
          .doGet(any[String])(headerCarrierNapper.capture)
      }
    }
  }
}
