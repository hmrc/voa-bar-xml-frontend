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

package controllers.actions

import identifiers.LoginId
import models.requests.DataRequest
import models.{CacheMap, Login, NormalMode}
import play.api.mvc.Results
import play.api.test.Helpers.*
import uk.gov.hmrc.vo.unit.test.BaseAppSpec
import utils.UserAnswers

import scala.concurrent.Future

class AuthActionSpec extends BaseAppSpec:

  private val sessionId = "session-ID"

  "AuthAction" should {

    "return unauthorised when user is not logged " in {
      val action = AuthAction()

      val req = DataRequest(getRequest, sessionId, UserAnswers(CacheMap(sessionId, Map())))

      val res = action.invokeBlock(
        req,
        _ => Future.failed(RuntimeException("This code should not be executed"))
      )
      status(res)           shouldBe TEMPORARY_REDIRECT
      redirectLocation(res) shouldBe Some(controllers.routes.LoginController.onPageLoad(NormalMode).url)
    }

    "return content of action when user is authorised " in {
      val action = AuthAction()

      val cacheMap = CacheMap(sessionId, Map(LoginId.toString -> Login.format.writes(Login("username", "password", None))))

      val req = DataRequest(getRequest, sessionId, UserAnswers(cacheMap))

      val res = action.invokeBlock(
        req,
        _ => Future.successful(Results.Ok("This is expected response"))
      )

      status(res)          shouldBe OK
      contentAsString(res) shouldBe "This is expected response"
    }
  }
