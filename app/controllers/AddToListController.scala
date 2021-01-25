/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.actions._
import identifiers.{AddPropertyId, VOAAuthorisedId}
import javax.inject.Inject
import journey.UniformJourney.Cr05SubmissionBuilder
import models.NormalMode
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.Navigator

import scala.concurrent.ExecutionContext
import models.YesNoForm._

class AddToListController @Inject()(appConfig: FrontendAppConfig,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    navigator: Navigator,
                                    dataCacheConnector: DataCacheConnector,
                                    controllerComponents: MessagesControllerComponents,
                                    addToList: views.html.add_to_list
                                  )(implicit ec: ExecutionContext)
  extends FrontendController(controllerComponents) with I18nSupport {

  def onPageLoad = getData.async {
    implicit request =>
      dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap  { maybeCr05Submission =>
        dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString) map {
          case Some(username) => Ok(addToList(username, maybeCr05Submission.get))
          case None => Redirect(routes.LoginController.onPageLoad(NormalMode))
        }
      }
  }

  def addProperty = (getData andThen requireData) {
    implicit request =>
      yesNoForm.bindFromRequest.fold(
        formWithErrors => ???,
        success => {
          if (success.value.contains(true)){
            Redirect(navigator.nextPage(AddPropertyId, NormalMode)(request.userAnswers))
          } else {
            Redirect(routes.TaskListController.onPageLoad())
          }
        }
      )
  }

}
