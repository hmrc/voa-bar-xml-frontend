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

import connectors.FakeDataCacheConnector
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.partials.HtmlPartial
import views.html.{inPageFeedbackThankyou, inpagefeedback, inpagefeedbackNoLogin}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class FeedbackControllerSpec extends ControllerSpecBase with MockitoSugar {
  def notLoggedInController() = {
    FakeDataCacheConnector.resetCaptures()
    val encrypter = mock[Encrypter with Decrypter]
    when(encrypter.encrypt(any[PlainText])).thenReturn(Crypted("foo"))
    val sessionCookieCrypto = mock[SessionCookieCrypto]
    when(sessionCookieCrypto.crypto).thenReturn(encrypter)
    val http = mock[HttpClient]
    when(http.GET[HtmlPartial](any[String])(any[HttpReads[HtmlPartial]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future(HtmlPartial.Success(None, Html("<div/>"))))
    when(http.POSTForm[HttpResponse](any[String], any[Map[String, Seq[String]]])(any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future(HttpResponse(OK)))
    new FeedbackController(
      messagesApi,
      getEmptyCacheMap,
      frontendAppConfig,
      sessionCookieCrypto,
      http)
  }
  val url = "feedback.url"

  "FeedbackController" should {
    "return feedback page when requested" in {
      val result = notLoggedInController.inPageFeedback()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe inpagefeedback(Some(url), frontendAppConfig)(fakeRequest, messages, notLoggedInController.formPartialRetriever).toString
    }
    "be able to submit form" in {
      val result = notLoggedInController.sendBetaFeedbackToHmrc()(fakeRequest.withFormUrlEncodedBody(("foo" -> "bar")))

      status(result) mustBe SEE_OTHER
    }
    "return feedback not logged in page when requested" in {
      val result = notLoggedInController.inPageFeedbackNoLogin()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe inpagefeedbackNoLogin(Some(url), frontendAppConfig)(fakeRequest, messages, notLoggedInController.formPartialRetriever).toString
    }
    "be able to submit form when not logged in" in {
      val result = notLoggedInController.sendBetaFeedbackToHmrcNoLogin()(fakeRequest.withFormUrlEncodedBody(("foo" -> "bar")))

      status(result) mustBe SEE_OTHER
    }
    "be able to display thank you page" in {
      val result = notLoggedInController.inPageFeedbackThankyou()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe inPageFeedbackThankyou(Some(url), frontendAppConfig)(fakeRequest, messages).toString
    }
  }

}
