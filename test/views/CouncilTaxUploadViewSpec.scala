/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.FileUploadDataFormProvider
import models.UpScanRequests.{InitiateResponse, UploadRequest}
import views.behaviours.ViewBehaviours

class CouncilTaxUploadViewSpec extends ViewBehaviours with ViewSpecBase {

  def councilTaxUpload = app.injector.instanceOf[views.html.councilTaxUpload]

  val username = "BA0345"
  val messageKeyPrefix = "councilTaxUpload"
  val submissionId = "SId9324832"

  val form = new FileUploadDataFormProvider()()

  val councilTaxUploadFakeRequest = fakeRequest

  val initiateResponse = InitiateResponse(
    reference = "foo",
    uploadRequest = UploadRequest(
      href = "http://www.bar.foo",
      fields = Map(
        ("acl", "private"),
        ("key", "xxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"),
        ("policy", "xxxxxxxx=="),
        ("x-amz-algorithm", "AWS4-HMAC-SHA256"),
        ("x-amz-credential", "ASIAxxxxxxxxx/20180202/eu-west-2/s3/aws4_request"),
        ("x-amz-date", "yyyyMMddThhmmssZ"),
        ("x-amz-meta-callback-url", "https://myservice.com/callback"),
        ("x-amz-signature", "xxxx"),
        ("x-amz-meta-consuming-service", "something"),
        ("x-amz-meta-session-id", "session-1234567890"),
        ("x-amz-meta-request-id", "request-12345789")
      )
    )
  )
  private def createView(displayInitiateResponse: Boolean = true) = {
    if (displayInitiateResponse) {
      councilTaxUpload(username, form, Some(initiateResponse))(councilTaxUploadFakeRequest, messages)
    } else {
      councilTaxUpload(username, form)(councilTaxUploadFakeRequest, messages)
    }
  }

  lazy val doc = asDocument(createView())

  "CouncilTaxUpload view" must {
    behave like normalPage(() => createView(), messageKeyPrefix,  "title" ,"submit.button")

    "Include an username element displaying the BA name based on given BA Code" in {
      val user = doc.select("body > div > dl > div:nth-child(2) > dd").text
      user mustBe "Reading"
    }

    "Include a signout link which redirects the users to the login page" in {
      val href = doc.getElementsByClass("hmrc-sign-out-nav__link").first.attr("href")
      href mustBe controllers.routes.SignOutController.signOut().url
    }

    "contain Submit button with the value Upload" in {
      val doc = asDocument(createView())
      val submitButton = doc.getElementById("submit").text()
      submitButton mustBe messages("councilTaxUpload.submit.button")
    }

    "contain Upscan expected hidden inputs" in {
      val doc = asDocument(createView())
      val upscanInputs = doc.getElementById("councilTaxUploadForm").getElementsByAttributeValue("type", "hidden")
      Option(upscanInputs.select("[name='policy']")) mustBe defined
      upscanInputs.select("[name='policy']").`val` mustBe initiateResponse.uploadRequest.fields("policy")
      Option(upscanInputs.select("[name='x-amz-algorithm']")) mustBe defined
      upscanInputs.select("[name='x-amz-algorithm']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-algorithm")
      Option(upscanInputs.select("[name='x-amz-credential']")) mustBe defined
      upscanInputs.select("[name='x-amz-credential']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-credential")
      Option(upscanInputs.select("[name='x-amz-date']")) mustBe defined
      upscanInputs.select("[name='x-amz-date']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-date")
      Option(upscanInputs.select("[name='x-amz-meta-callback-url']")) mustBe defined
      upscanInputs.select("[name='x-amz-meta-callback-url']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-meta-callback-url")
      Option(upscanInputs.select("[name='x-amz-meta-consuming-service']")) mustBe defined
      upscanInputs.select("[name='x-amz-meta-consuming-service']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-meta-consuming-service")
      Option(upscanInputs.select("[name='x-amz-signature']")) mustBe defined
      upscanInputs.select("[name='x-amz-signature']").`val` mustBe initiateResponse.uploadRequest.fields("x-amz-signature")
      Option(upscanInputs.select("[name='acl']")) mustBe defined
      upscanInputs.select("[name='acl']").`val` mustBe initiateResponse.uploadRequest.fields("acl")
      Option(upscanInputs.select("[name='key']")) mustBe defined
      upscanInputs.select("[name='key']").`val` mustBe initiateResponse.uploadRequest.fields("key")
    }

    "do not contain Submit button when there is not initiate response" in {
      val doc = asDocument(createView(false))
      val submitButton = Option(doc.getElementById("submit"))
      submitButton mustBe None
    }
  }
}
