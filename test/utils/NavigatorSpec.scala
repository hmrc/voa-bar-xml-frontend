/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import base.SpecBase
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import controllers.routes
import identifiers._
import models._

class NavigatorSpec extends SpecBase with MockitoSugar {

  val navigator = new Navigator

  val mockUserAnswers = mock[UserAnswers]

  "Navigator" when {

    "in Normal mode" must {
      "go to Login page from an identifier that doesn't exist in the route map" in {
        case object UnknownIdentifier extends Identifier
        navigator.nextPage(UnknownIdentifier, NormalMode)(mock[UserAnswers]) mustBe routes.LoginController.onPageLoad(NormalMode)
      }

      "on a valid submit from Login page go to Welcome page" in {
        when(mockUserAnswers.login) thenReturn  Some(Login("username", "pass"))
        navigator.nextPage(LoginId, NormalMode) (mockUserAnswers) mustBe routes.WelcomeController.onPageLoad()
      }

      "on clicking Council Tax Upload link should redirect to Council Tax Start Page" in {
        WelcomeId.toString mustBe "welcome"
        navigator.nextPage(WelcomeId, NormalMode) (mockUserAnswers) mustBe routes.CouncilTaxStartController.onPageLoad()
      }

      "on clicking Council Tax Start now button should redirect to Council Tax Upload Page" in {
        CouncilTaxStartId.toString mustBe "counciltaxstart"
        navigator.nextPage(CouncilTaxStartId, NormalMode) (mockUserAnswers) mustBe routes.CouncilTaxUploadController.onPageLoad()
      }
    }

    "in Check mode" must {
      "go to CheckYourAnswers from an identifier that doesn't exist in the edit route map" in {
        case object UnknownIdentifier extends Identifier
        navigator.nextPage(UnknownIdentifier, CheckMode)(mock[UserAnswers]) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
