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

import connectors.{AuditService, RequestBuilderStub}
import forms.FeedbackForm.feedbackForm
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import views.html.feedback.{feedback, feedbackError, feedbackThx}

import java.net.URL
import scala.concurrent.ExecutionContext

/**
  * @author Yuriy Tumakha
  */
class FeedbackControllerSpec extends ControllerSpecBase with MockitoSugar:

  private val ec                   = inject[ExecutionContext]
  private val controllerComponents = inject[MessagesControllerComponents]
  private val servicesConfig       = inject[ServicesConfig]
  private val auditService         = inject[AuditService]
  private val feedbackView         = inject[feedback]
  private val feedbackThxView      = inject[feedbackThx]
  private val feedbackErrorView    = inject[feedbackError]

  private val httpClientV2Mock = mock[HttpClientV2]
  when(
    httpClientV2Mock.post(any[URL])(using any[HeaderCarrier])
  ).thenReturn(RequestBuilderStub(Right(OK), "OK"))

  private val feedbackController = new FeedbackController(
    servicesConfig,
    auditService,
    httpClientV2Mock,
    feedbackView,
    feedbackThxView,
    feedbackErrorView,
    controllerComponents
  )(using ec)

  "FeedbackController" should {
    "return feedback page when requested" in {
      val result = feedbackController.onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe feedbackView(feedbackForm)(using fakeRequest, messages).toString
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
      contentAsString(result) mustBe feedbackThxView()(using fakeRequest, messages).toString
    }

    "be able to display error page" in {
      val result = feedbackController.feedbackError(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe feedbackErrorView()(using fakeRequest, messages).toString
    }
  }
