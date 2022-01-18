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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import identifiers.LoginId
import models.Login
import models.requests.DataRequest
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc.{MessagesControllerComponents, Results}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.UserAnswers
import views.html.unauthorised

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AuthActionSpec extends SpecBase with MockitoSugar with ScalaFutures with Injecting {

  val sessionId = "session-ID"

  "AuthAction" should {

    "return unauthorised when user is not logged " in {
      val action = new AuthAction(inject[MessagesControllerComponents], inject[FrontendAppConfig], inject[unauthorised])

      val req = DataRequest(fakeRequest, sessionId, new UserAnswers(new CacheMap(sessionId, Map())))

      val res = action.invokeBlock(req, { req: DataRequest[_] =>
        Future.failed(new RuntimeException("This code should not be executed"))
      })
      status(res) mustBe UNAUTHORIZED
    }

    "return content of action when user is authorised " in {
      val action = new AuthAction(inject[MessagesControllerComponents], inject[FrontendAppConfig], inject[unauthorised])

      val cacheMap = new CacheMap(sessionId, Map(LoginId.toString -> Login.format.writes(Login("username", "password", None))))

      val req = DataRequest(fakeRequest, sessionId, new UserAnswers(cacheMap))

      val res = action.invokeBlock(req, { _: DataRequest[_] =>
        Future.successful(Results.Ok("This is expected response"))
      })

      status(res) mustBe OK
      contentAsString(res) mustBe "This is expected response"
    }
  }
}
