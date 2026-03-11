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

import connectors.FakeDataCacheConnector2
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalActionImpl}
import identifiers.{LoginId, VOAuthorisedId}
import models.requests.DataRequest
import models.{Login, NormalMode}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, BodyParsers, MessagesControllerComponents}
import play.api.test.Helpers.*
import play.api.test.Injecting
import uk.gov.hmrc.http.SessionKeys
import utils.UserAnswers
import views.ViewSpecBase
import views.html.{error_template, reportReason}

import scala.concurrent.ExecutionContext

class ReportReasonControllerSpec extends ControllerSpecBase with ViewSpecBase with Injecting:

  private val sessionId            = "session-id"
  private val username             = "BA1445"
  private val ec                   = inject[ExecutionContext]
  private val controllerComponents = inject[MessagesControllerComponents]
  private val configuration        = Configuration("feature.cr05.enabled" -> false)

  private def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  private def reportReasonController() =
    val dataRetrievalAction = DataRetrievalActionImpl(FakeDataCacheConnector2, inject[BodyParsers.Default])(using ec)

    FakeDataCacheConnector2.resetCaptures()
    FakeDataCacheConnector2.save[String](sessionId, VOAuthorisedId.toString, username)
    FakeDataCacheConnector2.save[Login](sessionId, LoginId.toString, Login(username = username, password = username, reference = None))

    ReportReasonController(
      inject[MessagesApi],
      FakeDataCacheConnector2,
      dataRetrievalAction,
      inject[DataRequiredAction],
      inject[AuthAction],
      inject[error_template],
      inject[reportReason],
      inject[Configuration],
      inject[MessagesControllerComponents]
    )(using ec)

  private def viewAsString() = inject[reportReason]

  "ReportReasonController" must {

    "return OK and the correct view for a GET" in {
      val req = fakeRequest.withSession(SessionKeys.sessionId -> sessionId)

      val result = reportReasonController().onPageLoad()(req)
      status(result) mustBe OK

      val dataRequest = DataRequest[AnyContent](req, sessionId, UserAnswers(FakeDataCacheConnector2.fetchMap(sessionId)))
      contentAsString(result) mustBe viewAsString()(ReportReasonController.form, true)(using dataRequest, inject[MessagesApi].preferred(req)).toString
    }

    "return redirect on successful form submission" in {
      val req = fakeRequest.withMethod("POST").withSession(SessionKeys.sessionId -> sessionId)
        .withFormUrlEncodedBody("reportReason" -> "AddProperty")

      val result = reportReasonController().onPageSubmit(req)
      status(result) mustBe SEE_OTHER
    }
  }
