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

package forms

import forms.validator.{DeskproEmailValidator, NameValidator}
import play.api.data.Form
import play.api.data.Forms.{boolean, default, mapping, number, optional, text}

/**
  * @author Yuriy Tumakha
  */
case class FeedbackForm(
  rating: Int,
  name: String,
  email: String,
  comments: String,
  afterSubmission: Boolean,
  submissionId: Option[String] = None
)

object FeedbackForm {

  val validRatings: Set[Int]                = (1 to 5).toSet
  val emailValidator: DeskproEmailValidator = DeskproEmailValidator()
  val nameValidator: NameValidator          = NameValidator()

  val feedbackForm: Form[FeedbackForm] = Form(
    mapping(
      "feedback-rating"   -> default(number, 0)
        .verifying("feedback.rating.error.required", _ > 0)
        .verifying("feedback.rating.error.invalid", num => num == 0 || validRatings(num)),
      "feedback-name"     -> default(text, "Anonymous user")
        .verifying("feedback.name.error.length", _.length <= 70)
        .verifying("feedback.name.error.invalid", name => nameValidator.validate(name)),
      "feedback-email"    -> default(text, "anonymous@anonymous.com")
        .verifying("feedback.email.error.length", _.length <= 255)
        .verifying("feedback.email.error.invalid", email => emailValidator.validate(email)),
      "feedback-comments" -> default(text, "")
        .verifying("feedback.comments.error.length", _.length <= 2000),
      "afterSubmission"   -> default(boolean, false),
      "submissionId"      -> optional(text)
    )(FeedbackForm.apply)(o => Some(Tuple.fromProductTyped(o)))
  )

  def initFeedbackAfterSubmission(submissionId: String): Form[FeedbackForm] =
    feedbackForm.fill(FeedbackForm(0, "Anonymous user", "anonymous@anonymous.com", "", true, Option(submissionId)))

}
