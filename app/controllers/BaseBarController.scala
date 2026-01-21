/*
 * Copyright 2026 HM Revenue & Customs
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
import config.FrontendAppConfig
import connectors.DataCacheConnector
import identifiers.LoginId
import models.{Error, Login, NormalMode}
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait BaseBarController extends FrontendBaseController with Logging {

  val dataCacheConnector: DataCacheConnector

  implicit val ec: ExecutionContext

  val errorTemplate: views.html.error_template

  private[controllers] def error(messages: Messages, appConfig: FrontendAppConfig)(implicit request: Request[?]) =
    errorTemplate(
      messages("error.internal_server_error.heading"),
      messages("error.internal_server_error.description")
    )(using request, messages)

  private[controllers] def cachedLogin(externalId: String): Future[Either[Result, Login]] =
    EitherT.fromOptionF(
      dataCacheConnector.getEntry[Login](externalId, LoginId.toString),
      Redirect(routes.LoginController.onPageLoad(NormalMode))
    ).value

  private[controllers] def cachedLoginError(externalId: String): Future[Either[Error, Login]] =
    EitherT.fromOptionF(
      dataCacheConnector.getEntry[Login](externalId, LoginId.toString),
      Error("Couldn't get user session", Seq(s"ExternalId: $externalId"))
    ).value

  private[controllers] def cachedLoginByReference(reference: String): Future[Either[Error, Login]] =
    EitherT.fromOptionF(
      dataCacheConnector.getEntryByField[Login]("data.login.reference", reference, LoginId.toString),
      Error("Couldn't get user session", Seq(s"Reference : $reference"))
    ).value

  private[controllers] def saveLogin(externalId: String, login: Login): Future[Either[Result, Unit]] =
    dataCacheConnector.save[Login](externalId, LoginId.toString, login)
      .map(_ => Right(()))
      .recover {
        case ex: Throwable =>
          val errorMessage = "Error while saving login"
          logger.error(s"$errorMessage\n${ex.getMessage}")
          Left(InternalServerError)
      }
}
