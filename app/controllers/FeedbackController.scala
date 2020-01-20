/*
 * Copyright 2020 HM Revenue & Customs
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

import actions.DataRetrievalAction
import config.FrontendAppConfig
import javax.inject.Inject
import play.api.Configuration
import play.api.mvc.Results._
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.partials._
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{HttpGet, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.filters.frontend.crypto.SessionCookieCrypto
import uk.gov.hmrc.play.bootstrap.http.HttpClient

class FeedbackController @Inject() (
  override val messagesApi: MessagesApi,
  getData: DataRetrievalAction,
  appConfig: FrontendAppConfig,
  sessionCookieCrypto: SessionCookieCrypto,
  http: HttpClient,
  controllerComponents: MessagesControllerComponents,
  serviceConfig: ServicesConfig,
  configuration: Configuration
)(implicit ec: scala.concurrent.ExecutionContext) extends FrontendController(controllerComponents) with I18nSupport  {
  implicit val formPartialRetriever: FormPartialRetriever = new FormPartialRetriever {
    override def httpGet: HttpGet = http
    override def crypto: (String) => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value
  }
  val contactFrontendPartialBaseUrl = serviceConfig.baseUrl("contact-frontend")
  val serviceIdentifier = "VOA_BAR"
  val serviceName = configuration.getString("appName")

  val betaFeedbackSubmitUrl = s"/$serviceName${routes.FeedbackController.sendBetaFeedbackToHmrc().url}"
  val betaFeedbackSubmitUrlNoLogin = s"/$serviceName${routes.FeedbackController.sendBetaFeedbackToHmrcNoLogin().url}"
  val hmrcSubmitBetaFeedbackUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?resubmitUrl=${urlEncode(betaFeedbackSubmitUrl)}"
  val hmrcSubmitBetaFeedbackNoLoginUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?resubmitUrl=${urlEncode(betaFeedbackSubmitUrlNoLogin)}"
  val hmrcBetaFeedbackFormUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?service=$serviceIdentifier&submitUrl=${urlEncode(betaFeedbackSubmitUrl)}"
  val hmrcBetaFeedbackFormNoLoginUrl = s"$contactFrontendPartialBaseUrl/contact/beta-feedback/form?service=$serviceIdentifier&submitUrl=${urlEncode(betaFeedbackSubmitUrlNoLogin)}"

  val hmrcHelpWithPageFormUrl = s"$contactFrontendPartialBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"


  // The default HTTPReads will wrap the response in an exception and make the body inaccessible
  implicit val readPartialsForm: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    def read(method: String, url: String, response: HttpResponse) = response
  }

  private def urlEncode(value: String) = URLEncoder.encode(value, "UTF-8")

  //override def crypto: String => String = cookie => sessionCookieCrypto.crypto.encrypt(PlainText(cookie)).value

  def inPageFeedback = Action { implicit request =>
        Ok(views.html.inpagefeedback(Some(hmrcBetaFeedbackFormUrl), appConfig, None))
  }

  def sendBetaFeedbackToHmrc = getData.async { implicit request =>
        request.body.asFormUrlEncoded.map { formData =>
          http.POSTForm[HttpResponse](hmrcSubmitBetaFeedbackUrl, formData) map { res => res.status match {
            case 200 => Redirect(routes.FeedbackController.inPageFeedbackThankyou)
            case 400 => BadRequest(views.html.inpagefeedback(None, appConfig, Some(Html(res.body))))
            case _ => InternalServerError(views.html.feedbackError(appConfig = appConfig))
          }
          }
        }.getOrElse(throw new Exception("Empty Feedback Form"))
  }

  def sendBetaFeedbackToHmrcNoLogin = Action.async { implicit request =>
    request.body.asFormUrlEncoded.map { formData =>
      http.POSTForm[HttpResponse](hmrcSubmitBetaFeedbackNoLoginUrl, formData) map { res => res.status match {
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
