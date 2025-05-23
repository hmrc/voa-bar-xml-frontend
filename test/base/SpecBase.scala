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

package base

import config.FrontendAppConfig
import models.{CacheMap, Login}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.{Token, TokenInfo}
import utils.UserAnswers

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite {

  def injector: Injector = app.injector

  def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest = {
    val csfrToken = Token("csrfToken", "FixedCSRFTOkenValueForTest")
    val req       = FakeRequest("", "")
    req.withAttrs(req.attrs.updated(Token.InfoAttr -> TokenInfo(csfrToken)))
  }

  def messages: Messages = messagesApi.preferred(fakeRequest)

  class FakeUserAnswers(loginValue: Login, cacheMap: CacheMap = new CacheMap("", Map())) extends UserAnswers(cacheMap) {

    override def login: Option[Login] = Some(loginValue)
  }

}
