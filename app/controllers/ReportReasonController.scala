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

import connectors.DataCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import journey.{AddProperty, ReasonReportType, RemoveProperty, SplitProperty}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.reportReason

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import ReportReasonController._
import play.api.Configuration

class ReportReasonController @Inject() (
                                         override val messagesApi: MessagesApi,
                                         val dataCacheConnector: DataCacheConnector,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         auth: AuthAction,
                                         val errorTemplate: views.html.error_template,
                                         report_reason: reportReason,
                                         config: Configuration,
                                         controllerComponents: MessagesControllerComponents)
(implicit val ec: ExecutionContext) extends FrontendController(controllerComponents) with BaseBarController with I18nSupport {

  val cr05FeatureEnabled = config.getOptional[Boolean]("feature.cr05.enabled").contains(true)

  def onPageLoad = (getData andThen requireData andThen auth).async { implicit request =>

    dataCacheConnector.getEntry[ReasonReportType](request.externalId, STORAGE_KEY).map { maybeReportReason =>
      val pageForm = maybeReportReason.map(form.fill).getOrElse(form)
      Ok(report_reason(pageForm, cr05FeatureEnabled))
    }
  }

  def onPageSubmit = (getData andThen requireData andThen auth).async { implicit request =>

    form.bindFromRequest().fold(
      formWithErrors => Future.successful(Ok(report_reason(formWithErrors, cr05FeatureEnabled))),
      reportReason => {
        dataCacheConnector.save(request.externalId, STORAGE_KEY, reportReason ).map { _ =>
          reportReason match {
            case AddProperty => Redirect(routes.UniformController.myJourney("ba-report"))
            case RemoveProperty => Redirect(routes.UniformController.myJourney("why-should-it-be-removed"))
            case SplitProperty => Redirect(routes.TaskListController.onPageLoad())
          }
        }
      }
    )
  }

}

object ReportReasonController {

  val STORAGE_KEY = "ReportReason"

  val form =  {
    import play.api.data._
    import play.api.data.Forms._

    Form(
      single(
        "reportReason"  -> of[ReasonReportType]
      )
    )
  }

}
