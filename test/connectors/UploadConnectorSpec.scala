/*
 * Copyright 2022 HM Revenue & Customs
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

import base.SpecBase
import models.UpScanRequests._
import models._
import org.mockito.ArgumentCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatest.matchers.must
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class UploadConnectorSpec extends SpecBase with MockitoSugar with must.Matchers {

  implicit def hc: HeaderCarrier = HeaderCarrier()

  def configuration = injector.instanceOf[Configuration]
  def environment = injector.instanceOf[Environment]

  def file = File.createTempFile("foo", "bar")
  def path = new PrintWriter(file) { write("<xml />"); close }

  def tempCreator = injector.instanceOf[TemporaryFileCreator]

  def tempFile: TemporaryFile = tempCreator.create(file.toPath)

  val upScanConfigPath = "microservice.services.upscan"
  val upScanConfig = configuration.get[Configuration](upScanConfigPath)
  val upScanCallBackUrlPath = "callback-url"
  val upScanCallBackUrl = upScanConfig.get[String](upScanCallBackUrlPath)
  val maximumFileSizePath = "max-file-size"
  val maximumFileSize = upScanConfig.get[Int](maximumFileSizePath)

  def servicesConfig = injector.instanceOf[ServicesConfig]

  val xmlUrl = "http://localhost:59145"
  val username = "user"
  val password = "pass"
  lazy val login = Login(username, password).encrypt(configuration)

  val submissionId = "SId3824832"

  def getHttpMock(returnedStatus: Int, returnedString: Option[String]) = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(any[String], any[JsValue], any[Seq[(String, String)]])(any[Writes[JsValue]], any[HttpReads[Any]],
      any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(HttpResponse(returnedStatus, None, Map(), returnedString))
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
        val bodyCaptor = ArgumentCaptor.forClass(classOf[VoaBarUpload])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(Status.OK, Some(submissionId))

        val connector = new UploadConnector(httpMock, configuration, servicesConfig, messagesApi)
        val userHeader = connector.generateUsernameHeader(username)
        val passHeader = connector.generatePasswordHeader(login.password)

        await(connector.sendXml(xmlUrl, login, submissionId))
        verify(httpMock)
          .POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(any[Writes[VoaBarUpload]], httpReadsNapper.capture,  headerCarrierNapper.capture,
            any[ExecutionContext])
        urlCaptor.getValue must endWith(s"${connector.baseSegment}upload")
        bodyCaptor.getValue mustBe VoaBarUpload(submissionId, xmlUrl)
        headersCaptor.getValue mustBe Seq(userHeader, passHeader)
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        val connector = new UploadConnector(getHttpMock(Status.OK, Some(submissionId)), configuration, servicesConfig, messagesApi)
        val result = await(connector.sendXml(xmlUrl, login, submissionId))
        result match {
          case Right(submissionValue) => submissionValue mustBe submissionId
          case Left(e) => assert(false)
        }
      }

      "return a failure representing the error when send method fails" in {
        val connector = new UploadConnector(getHttpMock(Status.INTERNAL_SERVER_ERROR, None), configuration, servicesConfig, messagesApi)
        val result = await(connector.sendXml(xmlUrl, login, submissionId))
        assert(result.isLeft)
      }

      "return a failure if the upload call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POST(any[String], any[VoaBarUpload], any[Seq[(String, String)]])(any[Writes[VoaBarUpload]], any[HttpReads[Any]],
          any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(new RuntimeException)
        val connector = new UploadConnector(httpMock, configuration, servicesConfig, messagesApi)
        val result = await(connector.sendXml(xmlUrl, login, submissionId))
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
        when(httpMock.POST[InitiateRequest, InitiateResponse](any[String], any[InitiateRequest], any[Seq[(String, String)]])
          (jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any[ExecutionContext])) thenReturn Future.successful(initiateResponse)
        val connector = new UploadConnector(httpMock, configuration, servicesConfig, messagesApi)

        val response = await(connector.initiate(initiateRequest))

        assert(response.isRight)
        response.right.map(_.reference mustBe reference)
        verify(httpMock, times(1))
          .POST[InitiateRequest, InitiateResponse](any[String], any[InitiateRequest], any[Seq[(String, String)]])(jsonWritesNapper.capture, httpReadsNapper.capture, headerCarrierNapper.capture, any[ExecutionContext])
      }
    }
  }
}
