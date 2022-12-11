/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.AuditService
import forms.FeedbackForm.feedbackForm
import org.mockito.quality.Strictness
import org.mockito.scalatest.MockitoSugar
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import views.html.feedback.{feedback, feedbackError, feedbackThx}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Yuriy Tumakha
 */
class FeedbackControllerSpec extends ControllerSpecBase with MockitoSugar {

  val ec = injector.instanceOf[ExecutionContext]
  val controllerComponents = injector.instanceOf[MessagesControllerComponents]
  val servicesConfig = injector.instanceOf[ServicesConfig]
  val auditService = injector.instanceOf[AuditService]
  val feedbackView = injector.instanceOf[feedback]
  val feedbackThxView = injector.instanceOf[feedbackThx]
  val feedbackErrorView = injector.instanceOf[feedbackError]

  val feedbackController = {
    val http = mock[DefaultHttpClient](withSettings.strictness(Strictness.LENIENT))

    when(http.POSTForm[HttpResponse](any[String], any[Map[String, Seq[String]]], any[Seq[(String, String)]])
      (any[HttpReads[HttpResponse]], any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future(HttpResponse(OK, "OK")))

    new FeedbackController(
      servicesConfig,
      auditService,
      http,
      feedbackView,
      feedbackThxView,
      feedbackErrorView,
      controllerComponents)(ec)
  }

  "FeedbackController" should {
    "return feedback page when requested" in {
      val result = feedbackController.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe feedbackView(feedbackForm)(fakeRequest, messages).toString
    }

    "return 303 redirect for valid form data" in {
      val result = feedbackController.onPageSubmit()(fakeRequest.withMethod("POST")
        .withFormUrlEncodedBody("feedback-rating" -> "5"))

      status(result) mustBe SEE_OTHER
    }

    "return 400 Bad Request for invalid form data" in {
      val result = feedbackController.onPageSubmit()(fakeRequest.withMethod("POST")
        .withFormUrlEncodedBody("foo" -> "bar"))

      status(result) mustBe BAD_REQUEST
    }

    "be able to display thank you page" in {
      val result = feedbackController.feedbackThx(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe feedbackThxView()(fakeRequest, messages).toString
    }

    "be able to display error page" in {
      val result = feedbackController.feedbackError(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe feedbackErrorView()(fakeRequest, messages).toString
    }
  }

}
