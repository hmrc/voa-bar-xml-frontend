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

import java.util.UUID

import connectors.{FakeDataCacheConnector, ReportStatusConnector}
import controllers.actions.{DataRequiredActionImpl, DataRetrievalActionImpl}
import identifiers.LoginId
import models.Login
import org.mockito.scalatest.MockitoSugar
import play.api.Configuration
import play.api.mvc.{AnyContent, BodyParser, BodyParsers, MessagesControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ReportDeleteControllerSpec extends ControllerSpecBase with MockitoSugar {

  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def configuration = app.injector.instanceOf[Configuration]

  def bodyParser = app.injector.instanceOf[BodyParsers.Default]

  def errorTemplateView =  app.injector.instanceOf[views.html.error_template]

  val login = Login("foo", "bar")

  "ReportDeleteController" must {
    "Delete report " in {

      val submissionId = UUID.randomUUID().toString
      FakeDataCacheConnector.resetCaptures()
      FakeDataCacheConnector.save[Login]("", LoginId.toString, login)

      val controller = new ReportDeleteController(configuration, FakeDataCacheConnector, fakeReportStatusConnector(),
        controllerComponents, errorTemplateView,new DataRetrievalActionImpl(FakeDataCacheConnector, bodyParser))
      val request = fakeRequest.withFormUrlEncodedBody("submissionId" -> submissionId).withSession(SessionKeys.sessionId -> "")
      val respose = controller.onPageSubmit().apply(request)

      status(respose) mustBe 200
    }
  }

  def fakeReportStatusConnector() = {
    val reportStatusConnectorMock = mock[ReportStatusConnector](withSettings.lenient())
    when(reportStatusConnectorMock.deleteByReference(any[String], any[Login])(any[HeaderCarrier])).thenReturn(Future.successful(Right(HttpResponse(200))))

    reportStatusConnectorMock
  }

}
