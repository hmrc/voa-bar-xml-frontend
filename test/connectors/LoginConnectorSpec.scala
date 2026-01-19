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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.*
import play.api.Configuration
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class LoginConnectorSpec extends SpecBase with MockitoSugar with must.Matchers:

  private val configuration  = injector.instanceOf[Configuration]
  private val servicesConfig = injector.instanceOf[ServicesConfig]

  private val username = "user"
  private val password = "pass"
  private val login    = Login(username, password).encrypt(configuration)

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def getHttpMock(returnedStatus: Int): HttpClientV2 =
    val httpClientV2Mock = mock[HttpClientV2]
    when(
      httpClientV2Mock.post(any[URL])(using any[HeaderCarrier])
    ).thenReturn(RequestBuilderStub(Right(returnedStatus), "{}"))
    httpClientV2Mock

  "Login Connector" must {

    "call the Microservice with the given JSON for username provided" in {
      val headerCarrierNapper = ArgumentCaptor.forClass(classOf[HeaderCarrier])
      val urlCaptor           = ArgumentCaptor.forClass(classOf[URL])

      val httpMock          = getHttpMock(OK)
      val headerCarrierStub = HeaderCarrier()

      val connector = new LoginConnector(httpMock, servicesConfig)
      await(connector.doLogin(login)(using headerCarrierStub))

      verify(httpMock).post(urlCaptor.capture)(using headerCarrierNapper.capture)

      urlCaptor.getValue.toString must endWith("/voa-bar/login")
      headerCarrierNapper.getValue.nsStamp mustBe headerCarrierStub.nsStamp
    }

    "return a 200 status when the doLogin method is successfull" in {
      val connector = new LoginConnector(getHttpMock(200), servicesConfig)
      val result    = await(connector.doLogin(login))
      result mustBe Success(200)
    }

    "return a failure representing the error when doLogin method fails" in {
      val connector = new LoginConnector(getHttpMock(500), servicesConfig)
      val result    = await(connector.doLogin(login))
      result.isFailure mustBe true
      result.toString mustBe Failure(RuntimeException("Received status of 500 from upstream service when logging in")).toString
    }

    "return a failure if http call throws an exception" in {
      val httpClientV2Mock = mock[HttpClientV2]
      when(
        httpClientV2Mock.post(any[URL])(using any[HeaderCarrier])
      ).thenReturn(RequestBuilderStub(Left(RuntimeException("Login failed.")), "{}"))

      val connector = new LoginConnector(httpClientV2Mock, servicesConfig)
      val result    = await(connector.doLogin(login))
      result.isFailure mustBe true
    }

  }
