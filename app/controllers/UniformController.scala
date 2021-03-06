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
import controllers.actions.DataRetrievalAction
import ltbs.uniform._
import interpreters.playframework._

import javax.inject.{Inject, Singleton}
import journey.UniformJourney
import journey.UniformJourney.{AskTypes, Cr05AddProperty, Cr05SubmissionBuilder, TellTypes, addPropertyHelper}
import models.PropertyType
import models.requests.OptionalDataRequest
import play.api.{Configuration, Logger}
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
      Logger.warn(s"externalID: ${externalId}, db: ${db}")
      dataCacheConnector.save(externalId, storageKey, db).map(_ => ())
    }

  }

  lazy val interpreter = new AutobarsInterpreter(this, messagesApi, pageChrome, govukInput, govukRadios, govukDateInput, cr05SubmissionConfirmation)

  def myInterpreter(implicit request: Request[_]): AutobarsInterpreter = {
    val results = new Results {
      override def Redirect(url: String, queryString: Map[String, Seq[String]], status: Int): Result = {
        super.Redirect(url, queryString ++ request.queryString, status)
      }
    }
    new AutobarsInterpreter(results, messagesApi, pageChrome, govukInput, govukRadios, govukDateInput, cr05SubmissionConfirmation)
  }

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

    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey).flatMap { maybeData =>

      val addCommonSectionProgram = addPropertyCommon[WM](create[TellTypes, AskTypes](messages(request)), maybeData.flatMap(_.cr05CommonSection))
      if (request.userAnswers.flatMap(_.login).isEmpty) {
        implicit val messages = cc.messagesApi.preferred(request)
        Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
      } else {
        addCommonSectionProgram.run(targetId, purgeStateUponCompletion = true) { cr05CommonSection =>
          dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { savedCr05SubmissionBuilder =>
            val cr05SubmissionBuilder = savedCr05SubmissionBuilder.fold(Cr05SubmissionBuilder(Some(cr05CommonSection), None, List(), None)) { existingCr05SubmissionBuilder =>
              existingCr05SubmissionBuilder.copy(cr05CommonSection = Some(cr05CommonSection))
            }
            dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05SubmissionBuilder).map { _ =>
              Redirect(routes.TaskListController.onPageLoad())
            }
          }
        }
      }
    }
  }


  def editPropertyJourney(targetId: String, propertyType: PropertyType, index: Int): Action[AnyContent] = propertyJourney(
    targetId, propertyType, Option(index))

  def propertyJourney(targetId: String, propertyType: PropertyType, index: Option[Int])= getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    getCr05Submission.flatMap { propertyBuilder =>

      val property = propertyType match {
        case PropertyType.EXISTING => propertyBuilder.propertyToBeSplit
        case PropertyType.PROPOSED => index.flatMap(x => propertyBuilder.splitProperties.lift(x))
      }
      runPropertyJourney(targetId, propertyType, property, index)
    }
  }

  def runPropertyJourney(targetId: String, propertyType: PropertyType, property: Option[Cr05AddProperty], index: Option[Int])(implicit request: OptionalDataRequest[AnyContent]) = {
    val interpreter = myInterpreter
    import interpreter._
    import UniformJourney._
    val addPropertyProgram = addPropertyHelper[WM](create[TellTypes, AskTypes](messages(request)), property)
    addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { cr05AddProperty =>
      updateProperty(propertyType, cr05AddProperty, index).map { _ =>
        propertyType match {
          case PropertyType.EXISTING => Redirect(routes.TaskListController.onPageLoad())
          case PropertyType.PROPOSED => Redirect(routes.AddToListController.onPageLoad())
        }
      }
    }

  }

  def updateProperty(propertyType: PropertyType, property: Cr05AddProperty, index: Option[Int])(implicit request: OptionalDataRequest[_] ) = {
    Logger.warn(s"updating property : ${propertyType}, ${index}")
    (propertyType, index) match {
      case (PropertyType.EXISTING, _) => getCr05Submission.map(x => x.copy(propertyToBeSplit = Some(property))).flatMap(storeCr05Submission)
      case (PropertyType.PROPOSED, None) => getCr05Submission.map(x => x.copy(splitProperties = x.splitProperties :+ property )).flatMap(storeCr05Submission)
      case (PropertyType.PROPOSED, Some(index)) => {
        getCr05Submission.map { builder =>
          val splitProperties = builder.splitProperties.updated(index, property)
          builder.copy(splitProperties = splitProperties)
        }.flatMap(storeCr05Submission)
      }
    }
  }

  def storeCr05Submission(submission: Cr05SubmissionBuilder)(implicit request: OptionalDataRequest[_]) = {
    dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, submission).map(_ => ())
  }

  def getCr05Submission(implicit request: OptionalDataRequest[_]): Future[Cr05SubmissionBuilder] = {
    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey)
      .map(_.getOrElse(Cr05SubmissionBuilder(None, None, List(), None)))
  }


  def addCommentJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    val addCommentsProgram = addComments(create[TellTypes, AskTypes](messages(request)))
    if(request.userAnswers.flatMap(_.login).isEmpty) {
      implicit val messages = cc.messagesApi.preferred(request)
      Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
    } else {
      addCommentsProgram.run(targetId, purgeStateUponCompletion = true) { comments =>
        dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap  { savedCr05Submission =>
          val cr05Submission = savedCr05Submission.fold(Cr05SubmissionBuilder(None, None, List(), comments)){ existingSubmission =>
            existingSubmission.copy(comments = comments)
          }
          dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05Submission).map { _ =>
              Redirect(routes.TaskListController.onPageLoad())
          }
        }
      }
    }
  }

  def cr05CheckAnswerJourney(targetId: String) = getData.async { implicit request: OptionalDataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { maybeCr05Submission =>
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
