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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import identifiers._
import models.{CheckMode, Mode, NormalMode, PropertyType}

@Singleton
class Navigator @Inject() () {

  private val routeMap: Map[Identifier, UserAnswers => Call] = Map(
    LoginId                    -> (_ => routes.WelcomeController.onPageLoad),
    WelcomeFormId              -> (_ => routes.UniformController.myJourney("ba-report")),
    CouncilTaxStartId          -> (_ => routes.CouncilTaxUploadController.onPageLoad()),
    TaskListId                 -> (_ => routes.TaskListController.onPageLoad),
    AddPropertyReportDetailsId -> (_ => routes.UniformController.addCommonSectionJourney("add-property-ba-report")),
    AddPropertyId              -> (_ => routes.UniformController.propertyJourney("add-property-UPRN", PropertyType.PROPOSED)),
    AddCommentId               -> (_ => routes.UniformController.addCommentJourney()),
    CheckYourAnswersId         -> (_ => routes.UniformController.cr05CheckAnswerJourney())
  )

  private val editRouteMap: Map[Identifier, UserAnswers => Call] =
    Map(
    )

  def nextPage(id: Identifier, mode: Mode): UserAnswers => Call = mode match {
    case NormalMode =>
      routeMap.getOrElse(id, _ => routes.LoginController.onPageLoad(NormalMode))
    case CheckMode  =>
      editRouteMap.getOrElse(id, _ => routes.CheckYourAnswersController.onPageLoad)
  }
}
