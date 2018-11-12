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
import config.FrontendAppConfig
import connectors.DataCacheConnector
import identifiers.LoginId
import models.{Error, Login, NormalMode}
import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.{Controller, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

trait BaseBarController extends Controller {
  val dataCacheConnector: DataCacheConnector
  implicit val ec: ExecutionContext
  private[controllers] def error(messages: Messages, appConfig: FrontendAppConfig)(implicit request: Request[_]) =
    views.html.error_template(messages("error.internal_server_error.title"), messages("error.internal_server_error.title"), messages("error.internal_server_error.description"), appConfig)(request, messages)
  private[controllers] def cachedLogin(externalId: String): Future[Either[Result, Login]] = {
    EitherT.fromOptionF(
      dataCacheConnector.getEntry[Login](externalId, LoginId.toString),
      Redirect(routes.LoginController.onPageLoad(NormalMode))
    ).value
  }
  private[controllers] def cachedLoginError(externalId: String): Future[Either[Error, Login]] = {
    EitherT.fromOptionF(
      dataCacheConnector.getEntry[Login](externalId, LoginId.toString),
      Error("Couldn't get user session", Seq())
    ).value
  }
  private[controllers] def cachedLoginByReference(reference: String): Future[Either[Error, Login]] = {
    EitherT.fromOptionF(
      dataCacheConnector.getEntryByField[Login]("data.login.reference", reference, LoginId.toString),
      Error("Couldn't get user session", Seq())
    ).value
  }
  private[controllers] def saveLogin(externalId: String, login: Login): Future[Either[Result, Unit.type]] = {
    dataCacheConnector.save[Login](externalId, LoginId.toString, login)
        .map(_ => Right(Unit))
        .recover{
          case ex: Throwable => {
            val errorMessage = "Error while saving login"
            Logger.error(s"$errorMessage\n${ex.getMessage}")
            Left(InternalServerError)
          }
        }
  }
}
