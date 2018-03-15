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

import base.SpecBase
import models._
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future
import scala.util.{Failure, Success}

class UploadConnectorSpec extends SpecBase with MockitoSugar {

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]

  val xmlContent = """<sample>xml</sample>"""
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

        await(connector.sendXml(xmlContent, login))

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.baseSegment}upload")
        bodyCaptor.getValue mustBe xmlContent
        headersCaptor.getValue mustBe Seq(connector.xmlContentTypeHeader, userHeader, passHeader)
      }

      "return a String representing the submissionId Id when the send method is successfull using login model and xml content" in {
        val connector = new UploadConnector(getHttpMock(200, Some(submissionId)), configuration, environment)
        val result = await(connector.sendXml(xmlContent, login))
        result match {
          case Success(submissionValue) => submissionValue mustBe submissionId
          case Failure(e) => assert(false)
        }
      }

      "return a failure representing the error when send method fails" in {
        val connector = new UploadConnector(getHttpMock(500, None), configuration, environment)
        val result = await(connector.sendXml(xmlContent, login))
        assert(result.isFailure)
      }

      "return a failure if the upload call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
          any[HeaderCarrier], any())) thenReturn Future.successful(new RuntimeException)
        val connector = new UploadConnector(httpMock, configuration, environment)
        val result = await(connector.sendXml(xmlContent, login))
        assert(result.isFailure)
      }
    }

  }
}
