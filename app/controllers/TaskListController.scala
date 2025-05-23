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
import identifiers.*
import journey.UniformJourney.Cr05SubmissionBuilder
import models.NormalMode
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Navigator

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaskListController @Inject() (
  appConfig: FrontendAppConfig,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  dataCacheConnector: DataCacheConnector,
  controllerComponents: MessagesControllerComponents,
  taskList: views.html.task_list
)(implicit ec: ExecutionContext
) extends FrontendController(controllerComponents)
  with I18nSupport {

  def onPageLoad: Action[AnyContent] = getData.async {
    implicit request =>
      dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { maybeCr05Submission =>
        dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString) map {
          case Some(username) => Ok(taskList(username, maybeCr05Submission))
          case None           => Redirect(routes.LoginController.onPageLoad(NormalMode))
        }
      }
  }

  def goToAddPropertyReportDetailPage: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(AddPropertyReportDetailsId, NormalMode)(request.userAnswers))
  }

  def goToAddPropertyPage: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(AddPropertyId, NormalMode)(request.userAnswers))
  }

  def goToAddComments: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(AddCommentId, NormalMode)(request.userAnswers))
  }

  def goToCheckYourAnswersPage: Action[AnyContent] = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(CheckYourAnswersId, NormalMode)(request.userAnswers))
  }

}
