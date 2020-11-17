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
import services.Cr01Cr03Service
import uk.gov.hmrc.govukfrontend.views.html.components.{govukDateInput, govukInput, govukRadios}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.govuk.{cr05SubmissionConfirmation, pageChrome}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UniformController @Inject()(messagesApi: MessagesApi,
                                  config: Configuration,
                                  pageChrome: pageChrome,
                                  govukInput: govukInput,
                                  govukRadios: govukRadios,
                                  govukDateInput: govukDateInput,
                                  dataCacheConnector: DataCacheConnector,
                                  getData: DataRetrievalAction,
                                  appConfig: FrontendAppConfig,
                                  cr01cr03Service: Cr01Cr03Service,
                                  cr05SubmissionConfirmation: cr05SubmissionConfirmation,
                                  cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  implicit val cr05FeatureEnabled = config.getOptional[Boolean]("feature.cr05.enabled").contains(true)

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
      dataCacheConnector.getEntry[DB](externalId, storageKey).map(_.getOrElse(Map[List[String], String]()))
    }

    def save(externalId: String, db: _root_.ltbs.uniform.interpreters.playframework.DB): Future[Unit] = {
      dataCacheConnector.save(externalId, storageKey, db).map(_ => ())
    }

  }

  lazy val interpreter = new AutobarsInterpreter(this, messagesApi, pageChrome, govukInput, govukRadios, govukDateInput, cr05SubmissionConfirmation)

  def myJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    val playProgram = ctTaxJourney[WM](create[TellTypes, AskTypes](messages(request)))
    if(request.userAnswers.flatMap(_.login).isEmpty) {
      implicit val messages = cc.messagesApi.preferred(request)
      Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
    } else {
      playProgram.run(targetId, purgeStateUponCompletion = true) { cr01cr03Submission: Cr01Cr03Submission =>
        cr01cr03Service.storeSubmission(cr01cr03Submission, request.userAnswers.get.login.get).map { submissionId =>
          Redirect(routes.ConfirmationController.onPageRefresh(submissionId.toString()))
        }
      }
    }
  }

  def addCommonSectionJourney(targetId: String)= getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    val addCommonSectionProgram = addPropertyCommon[WM](create[TellTypes, AskTypes](messages(request)))
    if(request.userAnswers.flatMap(_.login).isEmpty) {
      implicit val messages = cc.messagesApi.preferred(request)
      Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
    } else {
      addCommonSectionProgram.run(targetId, purgeStateUponCompletion = true) { cr05CommonSection =>
        dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap  { savedCr05SubmissionBuilder =>
          val cr05SubmissionBuilder = savedCr05SubmissionBuilder.fold(Cr05SubmissionBuilder(Some(cr05CommonSection), None, None)){ existingCr05SubmissionBuilder =>
            existingCr05SubmissionBuilder.copy(cr05CommonSection = Some(cr05CommonSection))
          }
          dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05SubmissionBuilder).map { _ =>
            Redirect(routes.TaskListController.onPageLoad())
          }
        }
      }
    }
  }

  def addPropertyJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    val addPropertyProgram = addPropertyHelper[WM](create[TellTypes, AskTypes](messages(request)))
    if(request.userAnswers.flatMap(_.login).isEmpty) {
      implicit val messages = cc.messagesApi.preferred(request)
      Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
    } else {
      addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { cr05AddProperty =>
        dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap  { savedCr05Submission =>
          val cr05Submission = savedCr05Submission.fold(Cr05SubmissionBuilder(None, Some(cr05AddProperty), None)){ existingSubmission =>

              if (existingSubmission.propertyToBeSplit.isEmpty){
                existingSubmission.copy(propertyToBeSplit = Some(cr05AddProperty))
              } else {
                val splitProperties =
                existingSubmission.splitProperties.fold(Some(List(cr05AddProperty))){ sp =>
                  Some(sp :+ cr05AddProperty)
                }
                existingSubmission.copy(splitProperties = splitProperties)
              }
          }
          dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05Submission).map { _ =>
            if (cr05Submission.propertyToBeSplit.isDefined && cr05Submission.splitProperties.exists(_.length <= 1)){
              Redirect(routes.AddToListController.onPageLoad())
            } else {
              Redirect(routes.TaskListController.onPageLoad())
            }
          }
        }
      }
    }
  }

  def cr05CheckAnswerJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    dataCacheConnector.getEntry[Cr05Submission](request.externalId, Cr05Submission.storageKey) flatMap { maybeCr05Submission =>
      maybeCr05Submission match {
        case None =>
          // TODO Log / Return some kind of error
          implicit val messages = cc.messagesApi.preferred(request)
          Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
        case Some(cr05Submission) =>
          if(request.userAnswers.flatMap(_.login).isEmpty) {
            implicit val messages = cc.messagesApi.preferred(request)
            Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
          } else {
            val addPropertyProgram = cr05CheckYourAnswers[WM](create[TellTypes, AskTypes](messages(request)))(cr05Submission)
            addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { _ =>
              Future.successful(Redirect(routes.WelcomeController.onPageLoad()))
            }
          }
      }
    }
  }

}
