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

package views.components

import controllers.{ControllerSpecBase, routes}
import models.NormalMode
import views.behaviours.ViewBehaviours
import views.html.components.service_info
import play.routing.Router.Tags._

class ServiceInfoSpec extends ViewBehaviours with ControllerSpecBase {

  val baCode = "AUser"
  val welcomeRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.WelcomeController"))
  val councilTaxStartRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.CouncilTaxStartController"))
  val councilTaxUploadRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.CouncilTaxUploadController"))
  val confirmationControllerRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.ConfirmationController"))
  val reportStatusControllerRequest = fakeRequest.copyFakeRequest(tags = fakeRequest.tags + (ROUTE_CONTROLLER -> "controllers.ReportStatusController"))

  def welcomeView = () => service_info(baCode)(welcomeRequest, messages)

  def councilTaxStartView = () => service_info(baCode)(councilTaxStartRequest, messages)

  def councilTaxUploadView = () => service_info(baCode)(councilTaxUploadRequest, messages)

  def confirmationView = () => service_info(baCode)(confirmationControllerRequest, messages)

  def reportStatusView = () => service_info(baCode)(reportStatusControllerRequest, messages)

  val doc1 = asDocument(welcomeView())
  val doc2 = asDocument(councilTaxStartView())
  val doc3 = asDocument(councilTaxUploadView())
  val doc4 = asDocument(confirmationView())
  val doc5 = asDocument(reportStatusView())

  "service_info must" must {

    "Include a navigation bar and the class of the following types 'beta-banner divider--top'" in {

      val navbar = doc1.getElementById("service-info-navbar").className
      navbar mustBe "beta-banner divider--top"
    }

    "Have an unordered list of type breadcrumb-nav__list" in {
      val navlist = doc1.getElementById("service-info-list").className
      navlist mustBe "breadcrumb-nav__list"
    }

    "Include an baCode element displaying the BA name based on given BA Code" in {
      val user = doc1.getElementById("username-element").text
      user mustBe baCode.toUpperCase
    }

    "Include a logout link which redirects the users to the login page" in {
      val href = doc1.getElementById("logout-link").attr("href")
      assert(href == controllers.routes.LoginController.onPageLoad(NormalMode).url.toString)
    }

    "Have a home-element in the top navigation bar when the request is from WelcomeController" in {
      val elem = doc1.getElementById("home-element").text
      elem mustBe "Home"
    }

    "Have a home-link in the top navigation bar which links to the welcome page when request is from CouncilTaxStartController and" +
      "display '> Council Tax' next to the home link" in {
      val href = doc2.getElementById("homelink").attr("href")
      val currentPageName = doc2.getElementById("council-tax-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> Council Tax"
    }

    "Have a home-link in the top navigation bar which links to the welcome page when request is from CouncilTaxUploadController and" +
      "display '> Upload' next to the home link" in {
      val href = doc3.getElementById("homelink").attr("href")
      val currentPageName = doc3.getElementById("upload-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> Upload"
    }

    "Have a home-link in the top navigation bar which links to the welcome page when request is from ConfirmationController and" +
      "display '> Confirmation' next to the home link" in {
      val href = doc4.getElementById("homelink").attr("href")
      val currentPageName = doc4.getElementById("confirmation-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> Confirmation"
    }

    "Have a home-link in the top navigation bar which links to the welcome page when request is from ReportStatusController and" +
      "display '> History' next to the home link" in {
      val href = doc5.getElementById("homelink").attr("href")
      val currentPageName = doc5.getElementById("status-element").text
      href mustBe routes.WelcomeController.onPageLoad().url.toString
      currentPageName mustBe "> History"
    }
  }
}
