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

import org.mockito.ArgumentCaptor
import org.mockito.scalatest.MockitoSugar
import play.api.{Configuration, Environment}
import play.api.libs.json._
import base.SpecBase
import models._
import org.scalatest.matchers.must

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class LoginConnectorSpec extends SpecBase with MockitoSugar with must.Matchers {

  def configuration = injector.instanceOf[Configuration]
  def environment = injector.instanceOf[Environment]
  def minimalJson = JsObject(Map[String, JsValue]())
  def servicesConfig = injector.instanceOf[ServicesConfig]

  val username = "user"
  val password = "pass"
  lazy val login = Login(username, password).encrypt(configuration)

  implicit def hc: HeaderCarrier = HeaderCarrier()

  def getHttpMock(returnedStatus: Int) = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(any[String], any[JsValue], any[Seq[(String, String)]])(any[Writes[JsValue]], any[HttpReads[Any]],
      any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(HttpResponse(returnedStatus, None))
//    when(httpMock.GET(any[String])(any[HttpReads[Any]], any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    httpMock
  }
  "Login Connector" when {

    "provided with a Contact Login Input" must {

      "call the Microservice with the given JSON for username provided" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[JsValue]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200)
        val connector = new LoginConnector(httpMock, configuration, servicesConfig)

        await(connector.send(login))

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any[ExecutionContext])

        urlCaptor.getValue must endWith(s"${connector.baseSegment}login")
        bodyCaptor.getValue mustBe Json.toJson(login)
        headersCaptor.getValue mustBe Seq(connector.jsonContentTypeHeader)
      }

      "return a 200 status when the send method is successfull using login model" in {
        val connector = new LoginConnector(getHttpMock(200), configuration, servicesConfig)
        val result = await(connector.send(login))
        result match {
          case Success(status) => status mustBe 200
          case Failure(e) => assert(false)
        }
      }

      "return a failure representing the error when send method fails" in {
        val connector = new LoginConnector(getHttpMock(500), configuration, servicesConfig)
        val result = await(connector.send(login))
        assert(result.isFailure)
      }

    }

    "provided with JSON directly" must {

      "call the Microservice with the given JSON" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[JsValue]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200)
        val connector = new LoginConnector(httpMock, configuration, servicesConfig)

        await(connector.sendJson(minimalJson))

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any[ExecutionContext])
        urlCaptor.getValue must endWith(s"${connector.baseSegment}login")
        bodyCaptor.getValue mustBe minimalJson
        headersCaptor.getValue mustBe Seq(connector.jsonContentTypeHeader)
      }

      "return a 200 status when the send method is successful" in {
        val connector = new LoginConnector(getHttpMock(200), configuration, servicesConfig)
        val result = await(connector.sendJson(minimalJson))
        result match {
          case Success(status) => status mustBe 200
          case Failure(e) => assert(false)
        }
      }

      "return failure respresenting the error if the backend service call fails using minimal Json" in {
        val connector = new LoginConnector(getHttpMock(500), configuration,servicesConfig)
        val result = await(connector.sendJson(minimalJson))
        assert(result.isFailure)
      }

      "return a failure if the data transfer call throws an exception" in {
        val httpMock = mock[HttpClient]
        when(httpMock.POST(any[String], any[JsValue], any[Seq[(String, String)]])(any[Writes[JsValue]], any[HttpReads[Any]],
          any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(new RuntimeException)
        val connector = new LoginConnector(httpMock, configuration,servicesConfig)
        val result = await(connector.sendJson(minimalJson))
        assert(result.isFailure)
      }
    }

  }
}
