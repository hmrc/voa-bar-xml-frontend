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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import ltbs.uniform._
import interpreters.playframework._

import javax.inject.{Inject, Singleton}
import journey.{ReasonReportType, UniformJourney}
import journey.UniformJourney.{AskTypes, Cr05AddProperty, Cr05SubmissionBuilder, TellTypes, addPropertyHelper}
import models.PropertyType
import models.requests.DataRequest
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
                                  requireData: DataRequiredAction,
                                  auth: AuthAction,
                                  appConfig: FrontendAppConfig,
                                  cr01cr03Service: Cr01Cr03Service,
                                  cr05SubmissionConfirmation: cr05SubmissionConfirmation,
                                  cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  implicit val cr05FeatureEnabled = config.getOptional[Boolean]("feature.cr05.enabled").contains(true)

  implicit val mongoPersistance: PersistenceEngine[DataRequest[AnyContent]] = new PersistenceEngine[DataRequest[AnyContent]]() {

    val storageKey = "CR03"

    import utils.Formats.uniformDBFormat

    override def apply(request: DataRequest[AnyContent])(
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

  def myJourney(targetId: String) = (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    request.userAnswers.cacheMap.getEntry[ReasonReportType](ReportReasonController.STORAGE_KEY).map { reportReason =>
      val playProgram = ctTaxJourney[WM](create[TellTypes, AskTypes](messages(request)), reportReason)

      playProgram.run(targetId, purgeStateUponCompletion = true) { cr01cr03Submission: Cr01Cr03Submission =>
        cr01cr03Service.storeSubmission(cr01cr03Submission, request.userAnswers.login.get).map { submissionId =>
          Redirect(routes.ConfirmationController.onPageRefresh(submissionId.toString()))
        }
      }
    }.getOrElse(Future.successful(Redirect(routes.ReportReasonController.onPageLoad())))

  }

  def addCommonSectionJourney(targetId: String)= (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey).flatMap { maybeData =>

      val addCommonSectionProgram = addPropertyCommon[WM](create[TellTypes, AskTypes](messages(request)), maybeData.flatMap(_.cr05CommonSection))
      addCommonSectionProgram.run(targetId, purgeStateUponCompletion = true) { cr05CommonSection =>
        dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { savedCr05SubmissionBuilder =>
          val cr05SubmissionBuilder = savedCr05SubmissionBuilder.fold(Cr05SubmissionBuilder(Some(cr05CommonSection), List(), List(), None)) { existingCr05SubmissionBuilder =>
            existingCr05SubmissionBuilder.copy(cr05CommonSection = Some(cr05CommonSection))
          }
          dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05SubmissionBuilder).map { _ =>
            Redirect(routes.TaskListController.onPageLoad())
          }
        }
      }
    }
  }


  def editPropertyJourney(targetId: String, propertyType: PropertyType, index: Int): Action[AnyContent] = propertyJourney(
    targetId, propertyType, Option(index))

  def propertyJourney(targetId: String, propertyType: PropertyType, index: Option[Int])= (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    getCr05Submission.flatMap { propertyBuilder =>

      val property = propertyType match {
        case PropertyType.EXISTING => index.flatMap(x => propertyBuilder.existingProperties.lift(x))
        case PropertyType.PROPOSED => index.flatMap(x => propertyBuilder.proposedProperties.lift(x))
      }

      runPropertyJourney(targetId, propertyType, property, index)
    }
  }

  def runPropertyJourney(targetId: String, propertyType: PropertyType, property: Option[Cr05AddProperty], index: Option[Int])(implicit request: DataRequest[AnyContent]) = {
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

  def updateProperty(propertyType: PropertyType, property: Cr05AddProperty, index: Option[Int])(implicit request: DataRequest[_] ) = {
    Logger.debug(s"updating property : ${propertyType}, ${index}")
    (propertyType, index) match {
      case (PropertyType.EXISTING, None) => getCr05Submission.map(x => x.copy(existingProperties = x.existingProperties :+ property )).flatMap(storeCr05Submission)
      case (PropertyType.EXISTING, Some(index)) => {
        getCr05Submission.map { builder =>
          val existingProperties = builder.existingProperties.updated(index, property)
          builder.copy(existingProperties = existingProperties)
        }.flatMap(storeCr05Submission)
      }
      case (PropertyType.PROPOSED, None) => getCr05Submission.map(x => x.copy(proposedProperties = x.proposedProperties :+ property )).flatMap(storeCr05Submission)
      case (PropertyType.PROPOSED, Some(index)) => {
        getCr05Submission.map { builder =>
          val splitProperties = builder.proposedProperties.updated(index, property)
          builder.copy(proposedProperties = splitProperties)
        }.flatMap(storeCr05Submission)
      }
    }
  }

  def storeCr05Submission(submission: Cr05SubmissionBuilder)(implicit request: DataRequest[_]) = {
    dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, submission).map(_ => ())
  }

  def getCr05Submission(implicit request: DataRequest[_]): Future[Cr05SubmissionBuilder] = {
    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey)
      .map(_.getOrElse(Cr05SubmissionBuilder(None, List(), List(), None)))
  }


  def addCommentJourney(targetId: String) = (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    getCr05Submission(request).flatMap { submission =>
      val addCommentsProgram = addComments(create[TellTypes, AskTypes](messages(request)), submission.comments)
      addCommentsProgram.run(targetId, purgeStateUponCompletion = true) { comments =>
        val cr05Submission = submission.copy(comments = comments)
        dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05Submission).map { _ =>
          Redirect(routes.TaskListController.onPageLoad())
        }
      }
    }

  }

  def cr05CheckAnswerJourney(targetId: String) = (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import interpreter._
    import UniformJourney._

    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { maybeCr05Submission =>
      maybeCr05Submission match {
        case None =>
          // TODO Log / Return some kind of error
          implicit val messages = cc.messagesApi.preferred(request)
          Future.successful(Unauthorized(views.html.unauthorised(appConfig)))
        case Some(cr05Submission) =>
          val addPropertyProgram = cr05CheckYourAnswers[WM](create[TellTypes, AskTypes](messages(request)))(cr05Submission)
          addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { _ =>
            Future.successful(Redirect(routes.WelcomeController.onPageLoad()))
          }

      }
    }
  }

}
