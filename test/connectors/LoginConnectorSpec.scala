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
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.vo.unit.test.BaseAppSpec

import java.net.URL
import scala.util.{Failure, Success}

class LoginConnectorSpec extends BaseAppSpec:

  private val username = "user"
  private val password = "pass"
  private val login    = Login(username, password).encrypt(configuration)

  "Login Connector" should {
    "call the Microservice with the given JSON for username provided" in {
      val headerCarrierNapper = captor[HeaderCarrier]
      val urlCaptor           = captor[URL]

      val httpMock = httpClientMock(POST, responseBody = "{}")

      LoginConnector(httpMock, servicesConfig).doLogin(login).futureValue

      verify(httpMock).post(urlCaptor.capture)(using headerCarrierNapper.capture)

      urlCaptor.getValue.toString            should endWith("/voa-bar/login")
      headerCarrierNapper.getValue.nsStamp shouldBe hc.nsStamp
    }

    "return a 200 status when the doLogin method is successfully" in {
      val connector = LoginConnector(httpClientMock(POST, responseBody = "{}"), servicesConfig)
      val result    = connector.doLogin(login).futureValue
      result shouldBe Success(200)
    }

    "return a failure representing the error when doLogin method fails" in {
      val connector = LoginConnector(httpClientMock(POST, responseStatus = INTERNAL_SERVER_ERROR, responseBody = "{}"), servicesConfig)
      val result    = connector.doLogin(login).futureValue
      result.isFailure shouldBe true
      result.toString  shouldBe Failure(RuntimeException("Received status of 500 from upstream service when logging in")).toString
    }

    "return a failure if http call throws an exception" in {
      val connector = LoginConnector(httpClientFailedMock(POST, returnFailure = RuntimeException("Login failed.")), servicesConfig)
      val result    = connector.doLogin(login).futureValue
      result.isFailure shouldBe true
    }
  }
