/*
 * Copyright 2018 HM Revenue & Customs
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

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.actions._
import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector}
import identifiers.{CouncilTaxStartId, VOAAuthorisedId}
import models.{NormalMode, ReportStatus}
import models.requests.OptionalDataRequest
import play.api.mvc.{AnyContent, Result}
import utils.Navigator
import views.html.councilTaxStart

import scala.concurrent.{ExecutionContext, Future}

class CouncilTaxStartController @Inject()(appConfig: FrontendAppConfig,
                                          override val messagesApi: MessagesApi,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          navigator: Navigator,
                                          dataCacheConnector: DataCacheConnector,
                                          reportStatusConnector: ReportStatusConnector
                                         ) (implicit ec: ExecutionContext) extends FrontendController with I18nSupport {

  def onPageLoad = getData.async {
    implicit request =>
      (for {
        username <- EitherT(getUsername(request))
        reportStatus <- EitherT(getReportStatuses(username))
      } yield(Ok(councilTaxStart(username, appConfig, reportStatus, reportStatus.headOption))))
        .valueOr(fallbackPage => fallbackPage)
  }

  private def getReportStatuses(username: String): Future[Either[Result, Seq[ReportStatus]]]  = {
    reportStatusConnector.get(username)
      .map(_.fold(
        error => Left(InternalServerError(error.code)),
        Right(_)
      ))
  }

  private def getUsername(request: OptionalDataRequest[AnyContent]): Future[Either[Result, String]] = {
    dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString)
        .map(Either.fromOption(_, Redirect(routes.LoginController.onPageLoad(NormalMode))))
  }

  def goToCouncilTaxUploadPage() = (getData andThen requireData) { implicit request =>
    Redirect(navigator.nextPage(CouncilTaxStartId, NormalMode)(request.userAnswers))
  }
}
