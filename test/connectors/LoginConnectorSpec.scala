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

import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import play.api.{Configuration, Environment}
import play.api.libs.json._
import base.SpecBase
import exceptions.JsonInvalidException
import models._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global

class LoginConnectorSpec extends SpecBase with MockitoSugar {

  def getHttpMock(returnedStatus: Int) = {
    val httpMock = mock[HttpClient]
    when(httpMock.POST(anyString, any[JsValue], any[Seq[(String, String)]])(any[Writes[Any]], any[HttpReads[Any]],
      any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    when(httpMock.GET(anyString)(any[HttpReads[Any]], any[HeaderCarrier], any())) thenReturn Future.successful(HttpResponse(returnedStatus, None))
    httpMock
  }

  val configuration = injector.instanceOf[Configuration]
  val environment = injector.instanceOf[Environment]
  val minimalJson = JsObject(Map[String, JsValue]())

  val username = "user"
  val password = "pass"
  val login = Login(username, password)

  "Login Connector" when {

    "provided with a Contact Login Input" must {
      "call the Microservice with the given JSON for username provided" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200)

        val connector = new LoginConnector(httpMock, configuration, environment)
        connector.send(login)

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.baseSegment}validate")
        bodyCaptor.getValue mustBe Json.toJson(login)
        headersCaptor.getValue mustBe Seq(connector.jsonContentTypeHeader)
      }

      "return a 200 status when the send method is successfull using login model" in {
        new LoginConnector(getHttpMock(200), configuration, environment).send(login).map {
          case Success(status) => status mustBe 200
          case Failure(e) => assert(false)
        }
      }

      "return a 401 status when the send method is unsuccessful due to invalid credentials" in {
        new LoginConnector(getHttpMock(401), configuration, environment).send(login).map {
          case Success(status) => status mustBe 401
          case Failure(e) => assert(false)
        }
      }

      "return a string representing the error when send method fails" in {
        val errorResponse = JsString("Something went wrong!")

        new LoginConnector(getHttpMock(500), configuration, environment).send(login).map {
          case Failure(e) => {
            e mustBe a[RuntimeException]
            e.getMessage() mustBe "Received status of 500 from upstream service"
          }
          case Success(_) => fail
        }
      }

      "return a failure if the backend service call fails using Login Model" in {
        new LoginConnector(getHttpMock(500), configuration, environment).send(login). map {f =>
          assert(f.isFailure)
        }
      }

      }

    "provided with JSON directly" must {

      "call the Microservice with the given JSON" in {
        implicit val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
        implicit val httpReadsNapper = ArgumentCaptor.forClass(classOf[HttpReads[Any]])
        implicit val jsonWritesNapper = ArgumentCaptor.forClass(classOf[Writes[Any]])
        val urlCaptor = ArgumentCaptor.forClass(classOf[String])
        val bodyCaptor = ArgumentCaptor.forClass(classOf[JsValue])
        val headersCaptor = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])
        val httpMock = getHttpMock(200)

        val connector = new LoginConnector(httpMock, configuration, environment)
        connector.sendJson(minimalJson)

        verify(httpMock).POST(urlCaptor.capture, bodyCaptor.capture, headersCaptor.capture)(jsonWritesNapper.capture,
          httpReadsNapper.capture, headerCarrierNapper.capture, any())
        urlCaptor.getValue must endWith(s"${connector.baseSegment}validate")
        bodyCaptor.getValue mustBe minimalJson
        headersCaptor.getValue mustBe Seq(connector.jsonContentTypeHeader)
      }

      "return a 200 status when the send method is successful" in {
        new LoginConnector(getHttpMock(200), configuration, environment).sendJson(minimalJson).map {
          case Success(status) => status mustBe 200
          case Failure(e) => assert(false)
        }
      }

      "return a string representing the error when send method fails" in {
        val errorResponse = JsString("Something went wrong!")

        new LoginConnector(getHttpMock(500), configuration, environment).sendJson(minimalJson).map {
          case Failure(exception) => {
            exception mustBe a[JsonInvalidException]
            exception.getMessage() mustBe "Received status of 500 from upstream service"
          }
          case Success(_) => fail
        }
      }

      "return failure if the backend service call fails using minimal Json" in {
        new LoginConnector(getHttpMock(500), configuration, environment).sendJson(minimalJson). map {f =>
          assert(f.isFailure)
        }
      }
    }
  }
}

