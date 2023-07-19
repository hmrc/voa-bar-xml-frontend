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

import connectors.FakeDataCacheConnector2
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalActionImpl}
import identifiers.{LoginId, VOAAuthorisedId}
import models.requests.DataRequest
import models.{Login, NormalMode}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContent, BodyParsers, MessagesControllerComponents}
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.http.SessionKeys
import utils.UserAnswers
import views.ViewSpecBase
import views.html.{error_template, reportReason}

import scala.concurrent.ExecutionContext

class ReportReasonControllerSpec extends ControllerSpecBase with ViewSpecBase with Injecting {

  val sessionId = "session-id"

  val username = "BA1445"

  def ec = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val configuration =  Configuration("feature.cr05.enabled" -> false)

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def reportReasonController() = {

    val dataRetrievalAction = new DataRetrievalActionImpl(FakeDataCacheConnector2, inject[BodyParsers.Default])(ec)

    FakeDataCacheConnector2.resetCaptures()
    FakeDataCacheConnector2.save[String](sessionId, VOAAuthorisedId.toString, username)
    FakeDataCacheConnector2.save[Login](sessionId, LoginId.toString, Login(username = username, password = username, reference = None))

    new ReportReasonController(inject[MessagesApi], FakeDataCacheConnector2, dataRetrievalAction, inject[DataRequiredAction],
      inject[AuthAction], inject[error_template], inject[reportReason], inject[Configuration], inject[MessagesControllerComponents])(ec)
  }

  def viewAsString() = inject[reportReason]

  "ReportReasonController" must {

    "return OK and the correct view for a GET" in {
      val req = fakeRequest.withSession(SessionKeys.sessionId -> sessionId)

      val result = reportReasonController().onPageLoad()(req)
      status(result) mustBe OK

      val dataRequest = new DataRequest[AnyContent](req, sessionId, new UserAnswers(FakeDataCacheConnector2.fetchMap(sessionId)))
      contentAsString(result) mustBe (viewAsString()(ReportReasonController.form, true)(dataRequest, inject[MessagesApi].preferred(req))).toString()
    }

    "return redirect on successful form submission" in {
      val req = fakeRequest.withMethod("POST").withSession(SessionKeys.sessionId -> sessionId)
        .withFormUrlEncodedBody("reportReason" -> "AddProperty")

      val result = reportReasonController().onPageSubmit(req)
      status(result) mustBe SEE_OTHER

    }
  }
}
