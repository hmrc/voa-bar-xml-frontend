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

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import identifiers._
import models.{CheckMode, Mode, NormalMode}
import play.api.Logger

@Singleton
class Navigator @Inject()() {

  val welcomeRouting: UserAnswers => Call = answers => {
    answers.submissionCategory match {
      case Some("council_tax_webform") => routes.UniformController.myJourney("ba-report")
      case Some("council_tax_xml_upload") => routes.CouncilTaxStartController.onPageLoad()
      case Some(option) => {
        Logger.warn(s"Navigation for welcome reached with unknown option $option by controller")
        throw new RuntimeException(s"Navigation for welcome reached with unknown option $option by controller")
      }
      case None => {
        Logger.warn("Navigation for welcome reached without selection of enquiry by controller")
        throw new RuntimeException("Navigation for welcome reached without selection of enquiry by controller")
      }
    }
  }

  private val routeMap: Map[Identifier, UserAnswers => Call] = Map(
    LoginId -> (_ => routes.WelcomeController.onPageLoad()),
    WelcomeId -> welcomeRouting,
    CouncilTaxStartId -> (_ => routes.CouncilTaxUploadController.onPageLoad())
  )

  private val editRouteMap: Map[Identifier, UserAnswers => Call] = Map(

  )

  def nextPage(id: Identifier, mode: Mode): UserAnswers => Call = mode match {
    case NormalMode =>
      routeMap.getOrElse(id, _ => routes.LoginController.onPageLoad(NormalMode))
    case CheckMode =>
      editRouteMap.getOrElse(id, _ => routes.CheckYourAnswersController.onPageLoad())
  }
}
