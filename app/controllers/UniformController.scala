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
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import journey.UniformJourney.{Cr05AddProperty, Cr05SubmissionBuilder}
import journey.{ReasonReportType, UniformJourney}
import ltbs.uniform.*
import ltbs.uniform.interpreters.playframework.*
import models.PropertyType
import models.requests.DataRequest
import play.api.i18n.{Messages as _, *}
import play.api.mvc.*
import play.api.{Configuration, Logger, Logging}
import services.Cr01Cr03Service
import uk.gov.hmrc.govukfrontend.views.html.components.{Action as _, *}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.govuk.{cr05SubmissionSummary, pageChrome}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UniformController @Inject() (
  messagesApi: MessagesApi,
  config: Configuration,
  pageChrome: pageChrome,
  govukInput: GovukInput,
  govukRadios: GovukRadios,
  govukDateInput: GovukDateInput,
  dataCacheConnector: DataCacheConnector,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  auth: AuthAction,
  appConfig: FrontendAppConfig,
  cr01cr03Service: Cr01Cr03Service,
  cr05SubmissionSummary: cr05SubmissionSummary,
  cc: MessagesControllerComponents
)(implicit ec: ExecutionContext
) extends FrontendController(cc)
  with Logging {

  implicit val cr05FeatureEnabled: Boolean = config.getOptional[Boolean]("feature.cr05.enabled").contains(true)

  implicit val mongoPersistance: PersistenceEngine[DataRequest[AnyContent]] = new PersistenceEngine[DataRequest[AnyContent]]() {

    val storageKey = "CR03"

    import utils.Formats.uniformDBFormat

    override def apply(
      request: DataRequest[AnyContent]
    )(
      f: DB => Future[(_root_.ltbs.uniform.interpreters.playframework.DB, Result)]
    ): Future[Result] =
      for
        db              <- load(request.externalId)
        (newdb, result) <- f(db)
        _               <- save(request.externalId, newdb)
      yield result

    def load(externalId: String): Future[_root_.ltbs.uniform.interpreters.playframework.DB] =
      dataCacheConnector.getEntry[DB](externalId, storageKey).map(_.getOrElse(Map[List[String], String]()))

    def save(externalId: String, db: _root_.ltbs.uniform.interpreters.playframework.DB): Future[Unit] = {
      logger.warn(s"externalID: $externalId, db: $db")
      dataCacheConnector.save(externalId, storageKey, db).map(_ => ())
    }

  }

  val interpreter: AutobarsInterpreter = new AutobarsInterpreter(this, messagesApi, pageChrome, govukInput, govukRadios, govukDateInput, cr05SubmissionSummary)

  def myJourney(targetId: String): Action[AnyContent] = (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import UniformJourney.*
    import interpreter.*

    request.userAnswers.cacheMap.getEntry[ReasonReportType](ReportReasonController.STORAGE_KEY).map { reportReason =>
      val playProgram = ctTaxJourney[WM](create[TellTypes, AskTypes](messages(request)), reportReason)

      playProgram.run(targetId, purgeStateUponCompletion = true) { cr01cr03Submission =>
        cr01cr03Service.storeSubmission(cr01cr03Submission, request.userAnswers.login.get).map { submissionId =>
          Redirect(routes.ConfirmationController.onPageRefresh(submissionId.toString))
        }
      }
    }.getOrElse(Future.successful(Redirect(routes.ReportReasonController.onPageLoad)))

  }

  def addCommonSectionJourney(targetId: String): Action[AnyContent] = (getData andThen requireData andThen auth).async {
    implicit request: DataRequest[AnyContent] =>
      import UniformJourney.*
      import interpreter.*

      dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey).flatMap { maybeData =>
        val addCommonSectionProgram = addPropertyCommon[WM](create[TellTypes, AskTypes](messages(request)), maybeData.flatMap(_.cr05CommonSection))
        addCommonSectionProgram.run(targetId, purgeStateUponCompletion = true) { cr05CommonSection =>
          dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap { savedCr05SubmissionBuilder =>
            val cr05SubmissionBuilder = savedCr05SubmissionBuilder.fold(Cr05SubmissionBuilder(Some(cr05CommonSection), List(), List(), None)) {
              existingCr05SubmissionBuilder =>
                existingCr05SubmissionBuilder.copy(cr05CommonSection = Some(cr05CommonSection))
            }
            dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05SubmissionBuilder).map { _ =>
              Redirect(routes.TaskListController.onPageLoad)
            }
          }
        }
      }
  }

  def editPropertyJourney(targetId: String, propertyType: PropertyType, index: Int): Action[AnyContent] =
    propertyJourney(
      targetId,
      propertyType,
      Option(index)
    )

  def propertyJourney(targetId: String, propertyType: PropertyType, index: Option[Int]): Action[AnyContent] = (getData andThen requireData andThen auth).async {
    implicit request: DataRequest[AnyContent] =>
      getCr05Submission.flatMap { propertyBuilder =>
        val property = propertyType match {
          case PropertyType.EXISTING => index.flatMap(x => propertyBuilder.existingProperties.lift(x))
          case PropertyType.PROPOSED => index.flatMap(x => propertyBuilder.proposedProperties.lift(x))
        }

        runPropertyJourney(targetId, propertyType, property, index)
      }
  }

  private def runPropertyJourney(
    targetId: String,
    propertyType: PropertyType,
    property: Option[Cr05AddProperty],
    index: Option[Int]
  )(implicit request: DataRequest[AnyContent]
  ): Future[Result] = {
    import UniformJourney.*
    import interpreter.*
    val addPropertyProgram = addPropertyHelper[WM](create[TellTypes, AskTypes](messages(request)), property, propertyType, index)
    addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { cr05AddProperty =>
      updateProperty(propertyType, cr05AddProperty, index).map { _ =>
        propertyType match {
          case PropertyType.EXISTING => Redirect(routes.TaskListController.onPageLoad)
          case PropertyType.PROPOSED => Redirect(routes.AddToListController.onPageLoad)
        }
      }
    }

  }

  private def updateProperty(propertyType: PropertyType, property: Cr05AddProperty, index: Option[Int])(implicit request: DataRequest[?]): Future[Unit] = {
    logger.debug(s"updating property : $propertyType, $index")
    (propertyType, index) match {
      case (PropertyType.EXISTING, None)        =>
        getCr05Submission.map(x => x.copy(existingProperties = x.existingProperties :+ property)).flatMap(storeCr05Submission)
      case (PropertyType.EXISTING, Some(index)) =>
        getCr05Submission.map { builder =>
          val existingProperties = builder.existingProperties.updated(index, property)
          builder.copy(existingProperties = existingProperties)
        }.flatMap(storeCr05Submission)
      case (PropertyType.PROPOSED, None)        =>
        getCr05Submission.map(x => x.copy(proposedProperties = x.proposedProperties :+ property)).flatMap(storeCr05Submission)
      case (PropertyType.PROPOSED, Some(index)) =>
        getCr05Submission.map { builder =>
          val proposed = builder.proposedProperties.updated(index, property)
          builder.copy(proposedProperties = proposed)
        }.flatMap(storeCr05Submission)
    }
  }

  private def storeCr05Submission(submission: Cr05SubmissionBuilder)(implicit request: DataRequest[?]): Future[Unit] =
    dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, submission).map(_ => ())

  private def getCr05Submission(implicit request: DataRequest[?]): Future[Cr05SubmissionBuilder] =
    dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey)
      .map(_.getOrElse(Cr05SubmissionBuilder(None, List(), List(), None)))

  def addCommentJourney(targetId: String): Action[AnyContent] = (getData andThen requireData andThen auth).async { implicit request: DataRequest[AnyContent] =>
    import UniformJourney.*
    import interpreter.*

    getCr05Submission(using request).flatMap { submission =>
      val addCommentsProgram = addComments(create[TellTypes, AskTypes](messages(request)), submission.comments)
      addCommentsProgram.run(targetId, purgeStateUponCompletion = true) { comments =>
        val cr05Submission = submission.copy(comments = comments)
        dataCacheConnector.save(request.externalId, Cr05SubmissionBuilder.storageKey, cr05Submission).map { _ =>
          Redirect(routes.TaskListController.onPageLoad)
        }
      }
    }

  }

  def cr05CheckAnswerJourney(targetId: String): Action[AnyContent] = (getData andThen requireData andThen auth).async {
    implicit request: DataRequest[AnyContent] =>
      import UniformJourney.*
      import interpreter.*

      dataCacheConnector.getEntry[Cr05SubmissionBuilder](request.externalId, Cr05SubmissionBuilder.storageKey) flatMap {
        case None                 =>
          Logger("CheckAnswerJourney")
            .warn(s"Reach CR05 confirmation without finishing CR05, username: ${request.userAnswers.login.map(_.username).getOrElse("Unknown")}")
          Future.successful(Redirect(routes.TaskListController.onPageLoad))
        case Some(cr05Submission) =>
          val addPropertyProgram = cr05CheckYourAnswers[WM](create[TellTypes, AskTypes](messages(request)))(cr05Submission)
          addPropertyProgram.run(targetId, purgeStateUponCompletion = true) { _ =>
            cr01cr03Service.storeSubmission(cr05Submission.toCr05Submission, request.userAnswers.login.get) flatMap { submissionId =>
              dataCacheConnector.remove(request.externalId, Cr05SubmissionBuilder.storageKey).map { _ =>
                Redirect(routes.ConfirmationController.onPageRefresh(submissionId.toString))
              }
            }
          }
      }

  }

}
