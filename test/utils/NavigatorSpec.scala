/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatestplus.mockito.MockitoSugar
import controllers.routes
import identifiers.*
import models.*
import org.mockito.Mockito.when

class NavigatorSpec extends SpecBase with MockitoSugar {

  val navigator = new Navigator

  val mockUserAnswers = mock[UserAnswers]

  val formUserAnswers   = new FakeUserAnswers(Login("", ""))
  val uploadUserAnswers = new FakeUserAnswers(Login("", ""))

  "Navigator" when {

    "in Normal mode" must {
      "go to Login page from an identifier that doesn't exist in the route map" in {
        case object UnknownIdentifier extends Identifier
        navigator.nextPage(UnknownIdentifier, NormalMode)(mock[UserAnswers]) mustBe routes.LoginController.onPageLoad(NormalMode)
      }

      "on a valid submit from Login page go to Welcome page" in {
        when(mockUserAnswers.login) thenReturn Some(Login("username", "pass"))
        navigator.nextPage(LoginId, NormalMode)(mockUserAnswers) mustBe routes.WelcomeController.onPageLoad
      }

      "on choosing Council Tax web form should redirect to web form Start Page" in {
        WelcomeFormId.toString mustBe "welcomeForm"
        navigator.nextPage(WelcomeFormId, NormalMode)(formUserAnswers) mustBe routes.UniformController.myJourney("ba-report")
      }

      "on selecting Council Tax Upload link should redirect to Council Tax Upload Page" in {
        CouncilTaxStartId.toString mustBe "counciltaxstart"
        navigator.nextPage(CouncilTaxStartId, NormalMode)(uploadUserAnswers) mustBe routes.CouncilTaxUploadController.onPageLoad()
      }

      "on selecting Add Property Report Detail Journey link should redirect to Add Property Journey Report Details Page" in {
        AddPropertyReportDetailsId.toString mustBe "addpropertyreportdetailsid"
        navigator.nextPage(AddPropertyReportDetailsId, NormalMode)(uploadUserAnswers) mustBe
          routes.UniformController.addCommonSectionJourney("add-property-ba-report")
      }

      "on selecting Add Property Journey link should redirect to Add Property Journey Page" in {
        AddPropertyId.toString mustBe "addproperty"
        navigator.nextPage(AddPropertyId, NormalMode)(uploadUserAnswers) mustBe routes.UniformController.propertyJourney(
          "add-property-UPRN",
          PropertyType.PROPOSED
        )
      }

      "on selecting Add Comments Journey link should redirect to Add Comment Journey Page" in {
        AddCommentId.toString mustBe "addcomment"
        navigator.nextPage(AddCommentId, NormalMode)(uploadUserAnswers) mustBe routes.UniformController.addCommentJourney()
      }

      "on selecting Task List should redirect to Task List Page" in {
        TaskListId.toString mustBe "tasklist"
        navigator.nextPage(TaskListId, NormalMode)(uploadUserAnswers) mustBe routes.TaskListController.onPageLoad
      }

      "on selecting Check your answer should redirect to Check your answer Page" in {
        CheckYourAnswersId.toString mustBe "checkyouranswer"
        navigator.nextPage(CheckYourAnswersId, NormalMode)(uploadUserAnswers) mustBe routes.UniformController.cr05CheckAnswerJourney()
      }
    }

    "in Check mode" must {
      "go to CheckYourAnswers from an identifier that doesn't exist in the edit route map" in {
        case object UnknownIdentifier extends Identifier
        navigator.nextPage(UnknownIdentifier, CheckMode)(mock[UserAnswers]) mustBe routes.CheckYourAnswersController.onPageLoad
      }
    }
  }
}
