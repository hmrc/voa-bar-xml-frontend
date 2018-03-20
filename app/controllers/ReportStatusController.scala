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

import javax.inject.Inject

import config.FrontendAppConfig
import connectors.{DataCacheConnector, ReportStatusConnector}
import controllers.actions._
import identifiers.VOAAuthorisedId
import models.{NormalMode, ReportStatus}
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.reportStatus

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ReportStatusController @Inject()(appConfig: FrontendAppConfig,
                                       override val messagesApi: MessagesApi,
                                       dataCacheConnector: DataCacheConnector,
                                       reportStatusConnector: ReportStatusConnector,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction
                                      ) extends FrontendController with I18nSupport {

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)

  def verifyResponse(json: JsValue): Either[String, Map[String, List[ReportStatus]]] = {

    val reportStatuses = json.asOpt[Map[String, List[ReportStatus]]]
    reportStatuses match {
      case Some(response) => Right(response)
      case None => Left("Unable to parse the response from the Report Status Connector")
    }
  }

  def onPageLoad() = getData.async {
    implicit request =>
      dataCacheConnector.getEntry[String](request.externalId, VOAAuthorisedId.toString) flatMap {
        case Some(username) =>
          reportStatusConnector.request(username) map {
            case Success(jsValue) =>
              verifyResponse(jsValue) match {
                case Right(response) => {
                  val sorted = response.map(x => (x._1, x._2.sortBy(_.created)))
                  Ok(reportStatus(username, appConfig, sorted))
                }
                case Left(ex) => {
                  Logger.warn(ex)
                  throw new RuntimeException(ex)
                }
              }
            case Failure(ex) => throw new RuntimeException(ex)
          }
        case None => Future.successful(Redirect(routes.LoginController.onPageLoad(NormalMode)))
      }
  }
}
