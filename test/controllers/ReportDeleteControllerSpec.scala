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

import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions.DataRetrievalActionImpl
import identifiers.LoginId
import models.Login
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, withSettings}
import org.mockito.quality.Strictness
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{BodyParsers, MessagesControllerComponents}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReportDeleteControllerSpec extends ControllerSpecBase with MockitoSugar:

  private def controllerComponents = inject[MessagesControllerComponents]

  private def bodyParser = inject[BodyParsers.Default]

  private def errorTemplateView = inject[views.html.error_template]

  private val login = Login("foo", "bar")

  "ReportDeleteController" must {
    "Delete report " in {

      val submissionId = UUID.randomUUID.toString
      FakeDataCacheConnector.resetCaptures()
      FakeDataCacheConnector.save[Login]("", LoginId.toString, login)

      val controller = ReportDeleteController(
        FakeDataCacheConnector,
        fakeReportStatusConnector(),
        controllerComponents,
        errorTemplateView,
        DataRetrievalActionImpl(FakeDataCacheConnector, bodyParser)
      )
      val request    = fakeRequest.withFormUrlEncodedBody("submissionId" -> submissionId).withSession(SessionKeys.sessionId -> "")
      val response   = controller.onPageSubmit.apply(request)

      status(response) mustBe 200
    }
  }

  private def fakeReportStatusConnector(): ReportStatusConnector =
    val reportStatusConnectorMock = mock[ReportStatusConnector](withSettings.strictness(Strictness.LENIENT))

    when(reportStatusConnectorMock.deleteByReference(any[String], any[Login])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(HttpResponse(OK, "OK"))))

    reportStatusConnectorMock
