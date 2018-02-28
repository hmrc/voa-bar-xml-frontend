/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.{DataCacheConnector, FakeDataCacheConnector}
import controllers.actions._
import identifiers.VOAAuthorisedId
import models.NormalMode
import play.api.libs.json.Format
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.reportStatus

import scala.concurrent.Future

class ReportStatusControllerSpec extends ControllerSpecBase {
  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap): ReportStatusController = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, "AUser")
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, dataRetrievalAction, new DataRequiredActionImpl)
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new ReportStatusController(frontendAppConfig, messagesApi, FakeDataCacheConnector, dataRetrievalAction, new DataRequiredActionImpl)
  }
  def viewAsString() = reportStatus(frontendAppConfig)(fakeRequest, messages).toString

  "ReportStatus Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
    }

    "if authorized must request the LoginConnector for reports currently associated with this account" in {}
  }
}




