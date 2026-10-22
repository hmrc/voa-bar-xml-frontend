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

import forms.FeedbackForm.feedbackForm
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.*
import uk.gov.hmrc.vo.service.connectors.AuditService
import views.html.feedback.{feedback, feedbackError, feedbackThx}

/**
  * @author Yuriy Tumakha
  */
class FeedbackControllerSpec extends ControllerSpecBase:

  private val controllerComponents = inject[MessagesControllerComponents]
  private val auditService         = inject[AuditService]
  private val feedbackView         = inject[feedback]
  private val feedbackThxView      = inject[feedbackThx]
  private val feedbackErrorView    = inject[feedbackError]

  private val feedbackController = FeedbackController(
    servicesConfig,
    auditService,
    httpClientMock(POST, responseBody = "OK"),
    feedbackView,
    feedbackThxView,
    feedbackErrorView,
    controllerComponents
  )(using ec)

  "FeedbackController" should {
    "return feedback page when requested" in {
      val result = feedbackController.onPageLoad()(getRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe feedbackView(feedbackForm)(using getRequest, messages).toString
    }

    "return 303 redirect for valid form data" in {
      val result = feedbackController.onPageSubmit()(
        postRequest.withFormUrlEncodedBody("feedback-rating" -> "5")
      )

      status(result) shouldBe SEE_OTHER
    }

    "return 400 Bad Request for invalid form data" in {
      val result = feedbackController.onPageSubmit()(
        postRequest.withFormUrlEncodedBody("foo" -> "bar")
      )

      status(result) shouldBe BAD_REQUEST
    }

    "be able to display thank you page" in {
      val result = feedbackController.feedbackThx(getRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe feedbackThxView()(using getRequest, messages).toString
    }

    "be able to display error page" in {
      val result = feedbackController.feedbackError(getRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe feedbackErrorView()(using getRequest, messages).toString
    }
  }
