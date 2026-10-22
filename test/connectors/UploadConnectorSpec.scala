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

import models.*
import models.UpScanRequests.*
import play.api.Configuration
import play.api.libs.json.*
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vo.unit.test.BaseAppSpec

import java.net.URL

class UploadConnectorSpec extends BaseAppSpec:

  private val upScanConfig      = configuration.get[Configuration]("microservice.services.upscan")
  private val upScanCallBackUrl = upScanConfig.get[String]("callback-url")
  private val maximumFileSize   = upScanConfig.get[Int]("max-file-size")

  private val xmlUrl       = "http://localhost:59145"
  private val login        = Login("user", "pass").encrypt(configuration)
  private val submissionId = "SId3824832"

  "Upload Connector" when {

    "provided with an encrypted Login Input and some xml content" should {
      "call the Microservice with the given xml and login details" in {
        val headerCarrierNapper = captor[HeaderCarrier]
        val urlCaptor           = captor[URL]

        val httpMock = httpClientMock(POST, responseBody = "{}")

        UploadConnector(httpMock, servicesConfig, messagesApi).sendXml(xmlUrl, login, submissionId).futureValue

        verify(httpMock).post(urlCaptor.capture)(using headerCarrierNapper.capture)

        urlCaptor.getValue.toString            should endWith("/voa-bar/upload")
        headerCarrierNapper.getValue.nsStamp shouldBe hc.nsStamp
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        val connector = UploadConnector(httpClientMock(POST, responseBody = submissionId), servicesConfig, messagesApi)
        val result    = connector.sendXml(xmlUrl, login, submissionId).futureValue

        result shouldBe Right(Json.toJson(submissionId).toString)
      }

      "return a failure representing the error when send method fails" in {
        val connector = UploadConnector(httpClientMock(POST, responseStatus = INTERNAL_SERVER_ERROR, responseBody = "{}"), servicesConfig, messagesApi)
        val result    = connector.sendXml(xmlUrl, login, submissionId).futureValue

        result.isLeft   shouldBe true
        result.toString shouldBe Left(
          Error("Error while uploading file", List("The submission hasn’t been processed properly, please contact BARS@voa.gsi.gov.uk."))
        ).toString
      }

      "return a failure if the upload call throws an exception" in {
        val connector = UploadConnector(httpClientFailedMock(POST, returnFailure = RuntimeException("Upload failed.")), servicesConfig, messagesApi)
        val result    = connector.sendXml(xmlUrl, login, submissionId).futureValue

        result.isLeft   shouldBe true
        result.toString shouldBe Left(
          Error("Error while uploading file", List("The submission hasn’t been processed properly, please contact BARS@voa.gsi.gov.uk."))
        ).toString
      }
    }

    "provided with the proper file restrictions" should {
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

        val headerCarrierNapper = captor[HeaderCarrier]
        val urlCaptor           = captor[URL]

        val httpMock  = httpClientMock(POST, responseBody = Json.toJson(initiateResponse))
        val connector = UploadConnector(httpMock, servicesConfig, messagesApi)
        val response  = connector.initiate(initiateRequest).futureValue

        verify(httpMock, times(1))
          .post(urlCaptor.capture)(using headerCarrierNapper.capture)

        urlCaptor.getValue.toString          shouldBe "http://localhost:9570/upscan/v2/initiate"
        headerCarrierNapper.getValue.nsStamp shouldBe hc.nsStamp

        response.isRight          shouldBe true
        response.map(_.reference) shouldBe Right(reference)
      }
    }
  }
