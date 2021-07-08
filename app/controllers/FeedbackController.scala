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

import java.net.URLEncoder

import config.FrontendAppConfig
import controllers.actions.DataRetrievalAction
import javax.inject.Inject
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpGet, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.partials._

import scala.concurrent.ExecutionContext

class FeedbackController @Inject()( getData: DataRetrievalAction,
                                    appConfig: FrontendAppConfig,
                                    sessionCookieCrypto: SessionCookieCrypto,
                                    http: HttpClient,
                                    controllerComponents: MessagesControllerComponents,
                                    serviceConfig: ServicesConfig,
                                    configuration: Configuration
                                  )(implicit ec: ExecutionContext, formPartialRetriever: FormPartialRetriever) extends FrontendController(controllerComponents) with I18nSupport {



  val contactFrontendPartialBaseUrl = serviceConfig.baseUrl("contact-frontend")
  val serviceIdentifier = "VOA_BAR"
  val serviceName = configuration.get[String]("appName")

  val betaFeedbackSubmitUrl = s"/$serviceName${routes.FeedbackController.sendBetaFeedbackToHmrc().url}"
  val betaFeedbackSubmitUrlNoLogin = s"/$serviceName${routes.FeedbackController.sendBetaFeedbackToHmrcNoLogin().url}"
  val hmrcSubmitBetaFeedbackUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?resubmitUrl=${urlEncode(betaFeedbackSubmitUrl)}"
  val hmrcSubmitBetaFeedbackNoLoginUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?resubmitUrl=${urlEncode(betaFeedbackSubmitUrlNoLogin)}"
  val hmrcBetaFeedbackFormUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?service=$serviceIdentifier&submitUrl=${urlEncode(betaFeedbackSubmitUrl)}"
  val hmrcBetaFeedbackFormNoLoginUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?service=$serviceIdentifier&submitUrl=${urlEncode(betaFeedbackSubmitUrlNoLogin)}"

  val hmrcHelpWithPageFormUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  def inPageFeedback = Action { implicit request =>
    Ok(views.html.inpagefeedback(Some(hmrcBetaFeedbackFormUrl), appConfig, None))
  }

  def sendBetaFeedbackToHmrc = getData.async { implicit request =>
    request.body.asFormUrlEncoded.map { formData =>
      http.POSTForm[HttpResponse](hmrcSubmitBetaFeedbackUrl, formData, Seq("Csrf-Token" -> "nocheck")) map { res =>
        res.status match {
          case 200 => Redirect(routes.FeedbackController.inPageFeedbackThankyou)
          case 400 => BadRequest(views.html.inpagefeedback(None, appConfig, Some(Html(res.body))))
          case _ => InternalServerError(views.html.feedbackError(appConfig = appConfig))
        }
      }
    }.getOrElse(throw new Exception("Empty Feedback Form"))
  }

  def sendBetaFeedbackToHmrcNoLogin = Action.async { implicit request =>
    request.body.asFormUrlEncoded.map { formData =>
      http.POSTForm[HttpResponse](hmrcSubmitBetaFeedbackNoLoginUrl, formData, Seq("Csrf-Token" -> "nocheck")) map { res =>
        res.status match {
          case 200 => Redirect(routes.FeedbackController.inPageFeedbackThankyou)
          case 400 => BadRequest(views.html.inpagefeedbackNoLogin(None, appConfig, Some(Html(res.body))))
          case _ => InternalServerError(views.html.feedbackError(appConfig = appConfig))
        }
      }
    }.getOrElse(throw new Exception("Empty Feedback Form"))
  }

  def inPageFeedbackNoLogin = Action { implicit request =>
    Ok(views.html.inpagefeedbackNoLogin(Some(hmrcBetaFeedbackFormNoLoginUrl), appConfig))
  }

  def inPageFeedbackThankyou = Action { implicit request =>
    Ok(views.html.inPageFeedbackThankyou(appConfig = appConfig))
  }
}

//scalastyle:off line.size.limit
