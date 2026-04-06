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

package config

import play.api.Configuration
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.vo.service.config.VOServiceConfig
import models.NormalMode

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject() (val configuration: Configuration) extends VOServiceConfig:

  val baCodes: Seq[String] = configuration.get[String]("baCodes").split(",").toSeq

  override def serviceLocalRoot: Call           = controllers.routes.LoginController.onPageLoad(NormalMode)
  override def serviceMenuHome: Call            = controllers.routes.WelcomeController.onPageLoad
  override def serviceMenuSignOut: Option[Call] = Some(controllers.routes.SignOutController.signOut)
  override def theFirstPage: Call               = controllers.routes.WelcomeController.onPageLoad
  override def feedbackPage: Call               = controllers.routes.FeedbackController.onPageLoad // TODO: Remove to use feedbackFrontendForm
  override def signOutCall: Option[Call]        = Some(controllers.routes.TimeoutController.onPageLoad)

  override def timeoutCall(using request: RequestHeader): Option[Call] = signOutCall

  override def isWelshTranslationAvailable: Boolean = false

  override def stylesheet: Option[Call] = Some(controllers.routes.Assets.versioned("stylesheets/app.min.css"))

  override def notificationBannerEnabledOn: Set[Call] = Set(
    controllers.routes.LoginController.onPageLoad(NormalMode),
    controllers.routes.WelcomeController.onPageLoad
  )

  override def timeoutDialogEnabledExcept: Set[Call] = Set(
    controllers.routes.LoginController.onPageLoad(NormalMode),
    controllers.routes.LoginController.onSubmit(NormalMode),
    controllers.routes.UnauthorisedController.onPageLoad,
    controllers.routes.SignOutController.signOut,
    controllers.routes.TimeoutController.onPageLoad,
    controllers.routes.SessionExpiredController.onPageLoad,
    controllers.routes.FeedbackController.onPageLoad,
    controllers.routes.FeedbackController.feedbackThx,
    controllers.routes.FeedbackController.feedbackError
  )
