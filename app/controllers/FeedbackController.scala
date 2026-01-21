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

import connectors.AuditService
import forms.FeedbackForm.feedbackForm
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.ws.WSBodyWritables.writeableOf_urlEncodedForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpReads, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.feedback.{feedback, feedbackError, feedbackThx}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author Yuriy Tumakha
  */
@Singleton
class FeedbackController @Inject() (
  servicesConfig: ServicesConfig,
  auditService: AuditService,
  httpClientV2: HttpClientV2,
  feedbackView: feedback,
  feedbackThxView: feedbackThx,
  feedbackErrorView: feedbackError,
  cc: MessagesControllerComponents
)(implicit ec: ExecutionContext
) extends FrontendController(cc)
  with I18nSupport
  with Logging:

  private val serviceIdentifier: String           = "VOA_BAR"
  private val contactFrontendBaseUrl: String      = servicesConfig.baseUrl("contact-frontend")
  private val contactFrontendPostFeedbackUrl: URL = url"$contactFrontendBaseUrl/contact/beta-feedback"
  private val csrfNocheckHeader                   = "Csrf-Token" -> "nocheck"

  def onPageLoad: Action[AnyContent] = Action { implicit request =>
    Ok(feedbackView(feedbackForm))
  }

  def onPageSubmit: Action[AnyContent] = Action.async { implicit request =>
    feedbackForm.bindFromRequest().fold(
      formWithErrors =>
        Future.successful {
          BadRequest(feedbackView(formWithErrors))
        },
      form => {
        auditService.sendFeedback(form)

        val data: Map[String, Seq[String]] = Map(
          "feedback-rating"   -> form.rating.toString,
          "feedback-name"     -> form.name,
          "feedback-email"    -> form.email,
          "feedback-comments" -> form.comments,
          "service"           -> serviceIdentifier,
          "canOmitComments"   -> "true",
          "referrer"          -> s"${request.host}${request.uri}",
          "csrfToken"         -> ""
        ).view.mapValues(Seq(_)).toMap

        httpClientV2.post(contactFrontendPostFeedbackUrl)
          .withBody(data)
          .setHeader(csrfNocheckHeader)
          .execute[HttpResponse]
          .map { r =>
            r.status match {
              case OK =>
                logger.info(s"Feedback successful: ${r.status} response from $contactFrontendPostFeedbackUrl")
                Redirect(routes.FeedbackController.feedbackThx)
              case _  =>
                logger.error(s"Feedback FAILED: ${r.status} response from $contactFrontendPostFeedbackUrl,\nparams: $data")
                Redirect(routes.FeedbackController.feedbackError)
            }
          }
      }
    )
  }

  def feedbackThx: Action[AnyContent] = Action { implicit request =>
    Ok(feedbackThxView())
  }

  def feedbackError: Action[AnyContent] = Action { implicit request =>
    Ok(feedbackErrorView())
  }
