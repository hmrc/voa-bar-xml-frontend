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

package views

import play.api.data.Form
import forms.LoginFormProvider
import models.{Login, NormalMode}
import views.behaviours.govuk.QuestionViewBehaviours

class LoginViewSpec extends QuestionViewBehaviours[Login]:

  private def login = inject[views.html.login]

  private val messageKeyPrefix = "login"

  override val form: Form[Login] = LoginFormProvider()()

  private def createView = () => login(form, NormalMode)(using fakeRequest, messages)

  private def createViewUsingForm = (form: Form[Login]) => login(form, NormalMode)(using fakeRequest, messages)

  "Login view" must {

    behave like normalPage(createView, messageKeyPrefix)
    behave like pageWithTextFields(createViewUsingForm, "username", "password")

    "contain Login button with the value Login" in {
      val doc         = asDocument(createViewUsingForm(form))
      val loginButton = doc.getElementById("submit").text()
      assert(loginButton == messages("site.login"))
    }
  }
