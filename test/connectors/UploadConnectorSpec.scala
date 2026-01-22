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

import base.SpecBase
import models.*
import models.UpScanRequests.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.must
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.libs.json.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class UploadConnectorSpec extends SpecBase with MockitoSugar with must.Matchers:

  private val configuration: Configuration   = inject[Configuration]
  private val servicesConfig: ServicesConfig = inject[ServicesConfig]
  private val messagesApi: MessagesApi       = inject[MessagesApi]

  private val upScanConfig      = configuration.get[Configuration]("microservice.services.upscan")
  private val upScanCallBackUrl = upScanConfig.get[String]("callback-url")
  private val maximumFileSize   = upScanConfig.get[Int]("max-file-size")

  private val xmlUrl       = "http://localhost:59145"
  private val login        = Login("user", "pass").encrypt(configuration)
  private val submissionId = "SId3824832"

  private def getHttpMock(returnedStatus: Int, returnedBody: String = ""): HttpClientV2 =
    val httpClientV2Mock = mock[HttpClientV2]
    when(
      httpClientV2Mock.post(any[URL])(using any[HeaderCarrier])
    ).thenReturn(RequestBuilderStub(Right(returnedStatus), returnedBody))
    httpClientV2Mock

  "Upload Connector" when {

    "provided with an encrypted Login Input and some xml content" must {

      "call the Microservice with the given xml and login details" in {
        val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        val urlCaptor           = ArgumentCaptor.forClass(classOf[URL])

        val httpMock          = getHttpMock(OK)
        val headerCarrierStub = HeaderCarrier()

        val connector = new UploadConnector(httpMock, servicesConfig, messagesApi)
        await(connector.sendXml(xmlUrl, login, submissionId)(using headerCarrierStub))

        verify(httpMock).post(urlCaptor.capture)(using headerCarrierNapper.capture)

        urlCaptor.getValue.toString must endWith("/voa-bar/upload")
        headerCarrierNapper.getValue.nsStamp mustBe headerCarrierStub.nsStamp
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        given HeaderCarrier = HeaderCarrier()

        val connector = new UploadConnector(getHttpMock(Status.OK, submissionId), servicesConfig, messagesApi)
        val result    = await(connector.sendXml(xmlUrl, login, submissionId))

        result mustBe Right(submissionId)
      }

      "return a failure representing the error when send method fails" in {
        given HeaderCarrier = HeaderCarrier()

        val connector = new UploadConnector(getHttpMock(Status.INTERNAL_SERVER_ERROR), servicesConfig, messagesApi)
        val result    = await(connector.sendXml(xmlUrl, login, submissionId))

        result.isLeft mustBe true
        result.toString mustBe Left(
          Error("Error while uploading file", List("The submission hasn’t been processed properly, please contact BARS@voa.gsi.gov.uk."))
        ).toString
      }

      "return a failure if the upload call throws an exception" in {
        given HeaderCarrier = HeaderCarrier()

        val httpClientV2Mock = mock[HttpClientV2]
        when(
          httpClientV2Mock.post(any[URL])(using any[HeaderCarrier])
        ).thenReturn(RequestBuilderStub(Left(new RuntimeException("Upload failed."))))

        val connector = new UploadConnector(httpClientV2Mock, servicesConfig, messagesApi)
        val result    = await(connector.sendXml(xmlUrl, login, submissionId))

        result.isLeft mustBe true
        result.toString mustBe Left(
          Error("Error while uploading file", List("The submission hasn’t been processed properly, please contact BARS@voa.gsi.gov.uk."))
        ).toString
      }
    }

    "provided with the proper file restrictions" must {
      "call UpScan initiate endpoint" in {
        val reference        = "11370e18-6e24-453e-b45a-76d3e32ea33d"
        val initiateRequest  = InitiateRequest(upScanCallBackUrl, maximumFileSize)
        val uploadUrl        = "http://upload.url"
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

        val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        val urlCaptor           = ArgumentCaptor.forClass(classOf[URL])

        val headerCarrierStub = HeaderCarrier()

        val httpMock  = getHttpMock(OK, Json.toJson(initiateResponse).toString)
        val connector = new UploadConnector(httpMock, servicesConfig, messagesApi)
        val response  = await(connector.initiate(initiateRequest)(using headerCarrierStub))

        verify(httpMock, times(1))
          .post(urlCaptor.capture)(using headerCarrierNapper.capture)

        urlCaptor.getValue.toString mustBe "http://localhost:9570/upscan/v2/initiate"
        headerCarrierNapper.getValue.nsStamp mustBe headerCarrierStub.nsStamp

        response.isRight mustBe true
        response.map(_.reference) mustBe Right(reference)
      }
    }
  }
