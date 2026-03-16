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

package views

import models.ReportStatus
import views.behaviours.ViewBehaviours

class ReportStatusViewSpec extends ViewBehaviours with ViewSpecBase:

  private def reportStatus = inject[views.html.reportStatus]

  private val username           = "BA0350"
  private val messageKeyPrefix   = "reportStatus"
  private val fakeTableFormatter = TableFormatter()

  private val reportStatusFakeRequest = fakeRequest

  private def createView(reportStatuses: Seq[ReportStatus] = Seq()) =
    () => reportStatus(username, reportStatuses, fakeTableFormatter)(using reportStatusFakeRequest, messages)

  private def doc(reportStatuses: Seq[ReportStatus] = Seq()) = asDocument(createView(reportStatuses)())

  "ReportStatus view" must {
    behave like normalPage(createView(), messageKeyPrefix)

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc().select("body > div > dl > div:nth-child(2) > dd").text
      user mustBe "Slough"
    }

    "Include a signout link which redirects the users to the login page" in {
      val href = doc().getElementsByClass("hmrc-sign-out-nav__link").first.attr("href")
      href mustBe controllers.routes.SignOutController.signOut.url
    }

  }
