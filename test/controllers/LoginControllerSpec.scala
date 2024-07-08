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

import connectors.{FakeDataCacheConnector, LoginConnector}
import controllers.actions.*
import forms.LoginFormProvider
import identifiers.{LoginId, VOAAuthorisedId}
import models.{CacheMap, Login, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeNavigator
import views.ViewSpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class LoginControllerSpec extends ControllerSpecBase with ViewSpecBase with MockitoSugar {

  var captures = Map[String, Any]()

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  val formProvider = new LoginFormProvider()
  val form         = formProvider()
  val validBACode  = "ba0114"

  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  def ec                   = app.injector.instanceOf[ExecutionContext]

  def login         = app.injector.instanceOf[views.html.login]
  val configuration = injector.instanceOf[Configuration]

  implicit def hc: HeaderCarrier = any[HeaderCarrier]

  val loginConnector = mock[LoginConnector]
  when(loginConnector.send(any[Login])).thenReturn(Future.successful(Success(OK)))

  val loginConnectorF = mock[LoginConnector]
  when(loginConnectorF.send(any[Login])).thenReturn(Future.successful(Failure(new RuntimeException("Received exception from upstream service"))))

  def controller(connector: LoginConnector, dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new LoginController(
      frontendAppConfig,
      messagesApi,
      FakeDataCacheConnector,
      new FakeNavigator(desiredRoute = onwardRoute),
      dataRetrievalAction,
      new DataRequiredActionImpl(ec),
      formProvider,
      connector,
      controllerComponents,
      login,
      configuration
    )
  }

  def viewAsString(form: Form[Login] = form) = login(frontendAppConfig, form, NormalMode)(fakeRequest, messages).toString

  "Login Controller" must {

    "return OK and the correct view for a GET" in {
      val result = controller(loginConnector).onPageLoad(NormalMode)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val validData       = Map(LoginId.toString -> Json.toJson(Login("username", "password")))
      val getRelevantData = new FakeDataRetrievalAction(Some(CacheMap(cacheMapId, validData)))

      val result = controller(loginConnector, getRelevantData).onPageLoad(NormalMode)(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(Login("username", "")))
    }

    "redirect to the next page when valid data is submitted" in {
      val postRequest = fakeRequest.withMethod("POST").withFormUrlEncodedBody(("username", validBACode), ("password", "value 2"))

      val result = controller(loginConnector).onSubmit(NormalMode)(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "logging in must cache an authorization token" in {
      val postRequest = fakeRequest.withMethod("POST").withFormUrlEncodedBody(("username", validBACode), ("password", "value 2"))

      val result = controller(loginConnector).onSubmit(NormalMode)(postRequest)
      status(result) mustBe SEE_OTHER
      FakeDataCacheConnector.getCapture(VOAAuthorisedId.toString) mustBe Some(validBACode)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val postRequest = fakeRequest.withMethod("POST").withFormUrlEncodedBody(("value", "invalid value"))
      val boundForm   = form.bind(Map("value" -> "invalid value"))

      val result = controller(loginConnector).onSubmit(NormalMode)(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when valid bacode is submitted but no Council Name can be found related to the bacode" in {
      val postRequest = fakeRequest.withMethod("POST").withFormUrlEncodedBody(("username", "ba0000"), ("password", "value"))
      val boundForm   =
        form
          .withError("username", messages("error.invalid_username"))
          .withError("password", messages("error.invalid_password"))

      val result = controller(loginConnector).onSubmit(NormalMode)(postRequest)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when the backend service call fails" in {
      val postRequest = fakeRequest.withMethod("POST").withFormUrlEncodedBody(("username", "value 1"), ("password", "value 2"))
      val boundForm   = form.bind(Map("username" -> "value 1", "password" -> "value2"))

      intercept[Exception] {
        val result = controller(loginConnectorF).onSubmit(NormalMode)(postRequest)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
        redirectLocation(result) mustBe Some(onwardRoute.url)
        FakeDataCacheConnector.getCapture(VOAAuthorisedId.toString) mustBe None
      }
    }
  }
}
