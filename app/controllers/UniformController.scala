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

package controllers

import config.FrontendAppConfig
import connectors.DataCacheConnector
import controllers.actions.DataRetrievalAction
import ltbs.uniform._
import interpreters.playframework._
import javax.inject.{Inject, Singleton}
import journey.UniformJourney
import models.requests.OptionalDataRequest
import play.api.Configuration
import play.api.i18n.{Messages => _, _}
import play.api.mvc._
import services.Cr03Service
import uk.gov.hmrc.govukfrontend.views.html.components.{govukDateInput, govukInput, govukRadios}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.govuk.pageChrome

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UniformController @Inject()(messagesApi: MessagesApi,
                                  config: Configuration,
                                  pageChrome: pageChrome,
                                  govukInput: govukInput,
                                  govukRadios: govukRadios,
                                  govukDateInput: govukDateInput,
                                  dataCaheConnector: DataCacheConnector,
                                  getData: DataRetrievalAction,
                                  appConfig: FrontendAppConfig,
                                  cr03Service: Cr03Service,
                                  cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  implicit val cr01FeatureEnabled = config.getOptional[Boolean]("feature.cr01.enabled").contains(true)

  implicit val mongoPersistance: PersistenceEngine[OptionalDataRequest[AnyContent]] = new PersistenceEngine[OptionalDataRequest[AnyContent]]() {

    val storageKey = "CR03"

    import utils.Formats.uniformDBFormat

    override def apply(request: OptionalDataRequest[AnyContent])(
      f: DB => Future[(_root_.ltbs.uniform.interpreters.playframework.DB, Result)]): Future[Result] = {

      for {
        db <- load(request.externalId)
        (newdb, result) <- f(db)
        _ <- save(request.externalId, newdb)
      }yield {
        result
      }
    }

    def load(externalId: String): Future[_root_.ltbs.uniform.interpreters.playframework.DB] = {
      dataCaheConnector.getEntry[DB](externalId, storageKey).map(_.getOrElse(Map[List[String], String]()))
    }

    def save(externalId: String, db: _root_.ltbs.uniform.interpreters.playframework.DB): Future[Unit] = {
      dataCaheConnector.save(externalId, storageKey, db).map(_ => ())
    }

  }

  lazy val interpreter = new AutobarsInterpreter(this, messagesApi, pageChrome, govukInput, govukRadios, govukDateInput)

  def myJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._


    val playProgram = ctTaxJourney[WM](create[TellTypes, AskTypes](messages(request)))(cr01FeatureEnabled)
    if(request.userAnswers.flatMap(_.login).isEmpty) {
      implicit val messages = cc.messagesApi.preferred(request)
      Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
    } else {
      playProgram.run(targetId, purgeStateUponCompletion = true) { cr03Submission: Cr03Submission =>
        cr03Service.storeSubmission(cr03Submission, request.userAnswers.get.login.get).map { submissionId =>
          Redirect(routes.ConfirmationController.onPageRefresh(submissionId.toString()))
        }

      }
    }
  }

}
