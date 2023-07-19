/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.http.{HttpClient, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.feedback.{feedback, feedbackError, feedbackThx}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * @author Yuriy Tumakha
 */
@Singleton
class FeedbackController @Inject()(servicesConfig: ServicesConfig,
                                   auditService: AuditService,
                                   http: HttpClient,
                                   feedbackView: feedback,
                                   feedbackThxView: feedbackThx,
                                   feedbackErrorView: feedbackError,
                                   cc: MessagesControllerComponents
                                  )(implicit ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with Logging {

  private val serviceIdentifier = "VOA_BAR"
  private val contactFrontendBaseUrl = servicesConfig.baseUrl("contact-frontend")
  private val contactFrontendPostFeedbackUrl = s"$contactFrontendBaseUrl/contact/beta-feedback"

  // The default HTTPReads will wrap the response in an exception and make the body inaccessible
  implicit val readPartialsForm: HttpReads[HttpResponse] = (method: String, url: String, response: HttpResponse) => response

  def onPageLoad = Action { implicit request =>
    Ok(feedbackView(feedbackForm))
  }

  def onPageSubmit = Action.async { implicit request =>
    feedbackForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful {
          BadRequest(feedbackView(formWithErrors))
        },
      form => {
        auditService.sendFeedback(form)

        val data: Map[String, Seq[String]] = Map(
          "feedback-rating" -> form.rating.toString,
          "feedback-name" -> form.name,
          "feedback-email" -> form.email,
          "feedback-comments" -> form.comments,
          "service" -> serviceIdentifier,
          "canOmitComments" -> "true",
          "referrer" -> s"${request.host}${request.uri}",
          "csrfToken" -> ""
        ).view.mapValues(Seq(_)).toMap

        val headers = Seq("Csrf-Token" -> "nocheck")

        http.POSTForm[HttpResponse](contactFrontendPostFeedbackUrl, data, headers).map { res =>
          res.status match {
            case OK =>
              logger.info(s"Feedback successful: ${res.status} response from $contactFrontendPostFeedbackUrl")
              Redirect(routes.FeedbackController.feedbackThx)
            case _ =>
              logger.error(s"Feedback FAILED: ${res.status} response from $contactFrontendPostFeedbackUrl,\nparams: $data")
              Redirect(routes.FeedbackController.feedbackError)
          }
        }
      }
    )
  }

  def feedbackThx = Action { implicit request =>
    Ok(feedbackThxView())
  }

  def feedbackError = Action { implicit request =>
    Ok(feedbackErrorView())
  }

}
