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

import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import connectors.{DataCacheConnector, LoginConnector}
import controllers.actions.*
import forms.LoginFormProvider
import identifiers.{LoginId, VOAuthorisedId}
import models.{BillingAuthorities, Login, Mode}
import play.api.{Configuration, Logging}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.{Navigator, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class LoginController @Inject() (
  override val messagesApi: MessagesApi,
  dataCacheConnector: DataCacheConnector,
  navigator: Navigator,
  getData: DataRetrievalAction,
  formProvider: LoginFormProvider,
  loginConnector: LoginConnector,
  controllerComponents: MessagesControllerComponents,
  login: views.html.login,
  configuration: Configuration
)(using ec: ExecutionContext
) extends FrontendController(controllerComponents)
  with I18nSupport
  with Logging:

  private val form: Form[Login] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = getData.async {
    implicit request =>
      val preparedForm = request.userAnswers.flatMap(_.login) match
        case None        => form
        case Some(value) =>
          val loginWithBlankedPassword = Login(value.username, "")
          form.fill(loginWithBlankedPassword)
      dataCacheConnector.remove(request.externalId, VOAuthorisedId.toString) map {
        _ => Ok(login(preparedForm, mode))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = getData.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[Login]) =>
          Future.successful(BadRequest(login(formWithErrors, mode))),
        value =>
          val encryptedLogin = value.encrypt(configuration)
          dataCacheConnector.save[Login](request.externalId, LoginId.toString, encryptedLogin) flatMap { cacheMap =>
            loginConnector.doLogin(encryptedLogin) flatMap {
              case Success(_) =>
                BillingAuthorities.find(value.username) match
                  case Some(_) =>
                    dataCacheConnector.save[String](request.externalId, VOAuthorisedId.toString, value.username) map {
                      _ => Redirect(navigator.nextPage(LoginId, mode)(UserAnswers(cacheMap)))
                    }
                  case None    =>
                    logger.warn("BA Code authorized by VO but no valid Council Name can be found")
                    val formWithLoginErrors =
                      form
                        .withError("username", Messages("error.invalid_username"))
                        .withError("password", Messages("error.invalid_password"))
                    Future.successful(BadRequest(login(formWithLoginErrors, mode)))
              case Failure(_) =>
                val formWithLoginErrors =
                  form
                    .withError("username", Messages("error.invalid_username"))
                    .withError("password", Messages("error.invalid_password"))
                Future.successful(BadRequest(login(formWithLoginErrors, mode)))
            }
          }
      )
  }
