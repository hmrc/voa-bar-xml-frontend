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

import cats.data.EitherT
import cats.implicits._
import connectors.{DataCacheConnector, ReportStatusConnector}
import controllers.actions.DataRetrievalAction
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.reportStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import ReportDeleteController.submissionId


@Singleton
class ReportDeleteController @Inject() ( configuration: Configuration,
                                         val dataCacheConnector: DataCacheConnector,
                                         reportStatusConnector: ReportStatusConnector,
                                         controllerComponents: MessagesControllerComponents,
                                         getData: DataRetrievalAction
  )(implicit val ec: ExecutionContext)
    extends FrontendController(controllerComponents) with BaseBarController with I18nSupport {

  val enabled = configuration.getOptional[String]("feature.delete.enable").map(x => Try(x.toBoolean).getOrElse(false)).getOrElse(false)

  def onPageSubmit() = getData.async { implicit request =>
    val reference = request.body.asFormUrlEncoded.get(submissionId).head
    (for {
      login <- EitherT(cachedLogin(request.externalId))
      deleteStatus <- EitherT(reportStatusConnector.deleteByReference(reference, login)(hc))
    }yield {
      Ok(
        s"""Response\n\n
           |Status code ${deleteStatus.status} \n
           |body: ${deleteStatus.body}\n
           |headers: ${deleteStatus.allHeaders.mkString("\n  ", "\n  ", "")}
           | """.stripMargin)
    }).valueOr(f => f)

  }
}

object ReportDeleteController {
  val submissionId = "submissionId"
}