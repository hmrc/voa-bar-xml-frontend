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

package controllers

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions.*
import identifiers.{CouncilTaxStartId, TaskListId, VOAAuthorisedId}
import models.NormalMode
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Navigator

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WelcomeController @Inject()(appConfig: FrontendAppConfig,
                                  config: Configuration,
                                  getData: DataRetrievalAction,
                                  requireData: DataRequiredAction,
                                  navigator: Navigator,
                                  dataCacheConnector: DataCacheConnector,
                                  controllerComponents: MessagesControllerComponents,
                                  welcome: views.html.welcome
                                 ) (implicit ec: ExecutionContext)
  extends FrontendController(controllerComponents) with I18nSupport {

  private val cr05FeatureEnabled = config.getOptional[Boolean]("feature.cr05.enabled").contains(true)


  def onPageLoad: Action[AnyContent] = getData.async {
    implicit request =>
      dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString) map {
        case Some(username) => Ok(welcome(appConfig, username, cr05FeatureEnabled))
        case None => Redirect(routes.LoginController.onPageLoad(NormalMode))
      }
  }

  def goToCouncilTaxUploadPage: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(CouncilTaxStartId, NormalMode)(request.userAnswers))
  }

  def goToTaskListLoadPage: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(TaskListId, NormalMode)(request.userAnswers))
  }
}
