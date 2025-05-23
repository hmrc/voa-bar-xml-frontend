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

import com.google.inject.Inject
import config.FrontendAppConfig
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.language.{LanguageController, LanguageUtils}

class LanguageSwitchController @Inject() (
  languageUtils: LanguageUtils,
  configuration: Configuration,
  appConfig: FrontendAppConfig,
  controllerComponents: MessagesControllerComponents
) extends LanguageController(languageUtils, controllerComponents) {

  override protected def fallbackURL: String = routes.WelcomeController.onPageLoad.url

  override protected def languageMap: Map[String, Lang] = appConfig.languageMap
}
