/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.FakeDataCacheConnector
import controllers.actions._
import identifiers.VOAAuthorisedId
import journey.UniformJourney.{Address, ContactDetails, Cr05AddProperty, Cr05SubmissionBuilder}
import models.NormalMode
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.{Token, TokenInfo}
import utils.FakeNavigator
import views.ViewSpecBase

import scala.concurrent.{ExecutionContext, Future}

class AddToListControllerSpec extends ControllerSpecBase with ViewSpecBase  {

  val username = "AUser"


  def ec = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    new AddToListController(frontendAppConfig, dataRetrievalAction, new DataRequiredActionImpl(ec),
      new FakeNavigator(desiredRoute = onwardRoute), FakeDataCacheConnector, controllerComponents, createAddToListView())(ec)
  }

  def loggedInControllerWithSubmission(submission: Cr05SubmissionBuilder, dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    FakeDataCacheConnector.save[Cr05SubmissionBuilder]("", Cr05SubmissionBuilder.storageKey, submission)
    new AddToListController(frontendAppConfig, dataRetrievalAction, new DataRequiredActionImpl(ec),
      new FakeNavigator(desiredRoute = onwardRoute), FakeDataCacheConnector, controllerComponents, createAddToListView())(ec)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new AddToListController(frontendAppConfig,  dataRetrievalAction, new DataRequiredActionImpl(ec),
      new FakeNavigator(desiredRoute = onwardRoute), FakeDataCacheConnector, controllerComponents, createAddToListView())(ec)
  }

  def viewAsString(submission: Cr05SubmissionBuilder) = createAddToListView()(username, submission)(fakeRequest, messages).toString

  def formfakeRequest(formResponse: String) = {
    val csfrToken = Token("csrfToken", "FixedCSRFTOkenValueForTest")
    val req = FakeRequest("", "").withFormUrlEncodedBody("add-another" -> formResponse)
    req.withAttrs(req.attrs + (Token.InfoAttr -> TokenInfo(csfrToken)))
  }

  "AddToListController" must {

    "return OK and the correct view for a GET for submission with 1 split property" in {
      val addProperty = Cr05AddProperty(None, Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX"),
        ContactDetails("firstName", "lastName", Option("user@example.com"), Option("01122554442")),
        false, Option(Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX")),
        true, Option("1122"), None)
      val propertyToSplitSubmission = Cr05SubmissionBuilder(None, None, Some(List(addProperty)), None)
      val result: Future[Result] = loggedInControllerWithSubmission(propertyToSplitSubmission).onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(propertyToSplitSubmission)
    }

    "return OK and the correct view for a GET for submission with 2 split property" in {
      val addProperty = Cr05AddProperty(None, Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX"),
        ContactDetails("firstName", "lastName", Option("user@example.com"), Option("01122554442")),
        false, Option(Address("line1", "line2", Option("line3"), Option("line 4"), "BN12 4AX")),
        true, Option("1122"), None)
      val propertyToSplitSubmission = Cr05SubmissionBuilder(None, None, Some(List(addProperty, addProperty)), None)
      val result: Future[Result] = loggedInControllerWithSubmission(propertyToSplitSubmission).onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString(propertyToSplitSubmission)
    }


    "go to Add Property Page on POST with yes " in {
      val result: Future[Result] = loggedInController().addProperty(formfakeRequest("true"))

      status(result) mustBe SEE_OTHER
    }

    "go to Add Property Page on POST with no " in {
      val result: Future[Result] = loggedInController().addProperty(formfakeRequest("false"))

      status(result) mustBe SEE_OTHER
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

  }
}
