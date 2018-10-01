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

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import connectors.{FakeDataCacheConnector, ReportStatusConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.{LoginId, VOAAuthorisedId}
import models.UpScanRequests.{InitiateRequest, InitiateResponse, UploadConfirmation, UploadRequest}
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.councilTaxUpload

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class CouncilTaxUploadControllerSpec extends ControllerSpecBase with MockitoSugar {

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  val formProvider = new FileUploadDataFormProvider()
  val form = formProvider()
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val username = "BA0114"
  val password = "pass"
  lazy val login = Login(username, password).encrypt
  lazy val pathForValidFile = getClass.getResource("/valid.xml")

  val submissionId = "SID38273"
  val reference = submissionId
  val uploadUrl = "http://foo.bar"
  val userReport = UserReportUpload(reference, username, password)
  val initiateResponse = InitiateResponse(
    reference = reference,
    uploadRequest = UploadRequest(
      href = uploadUrl,
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
  val error = Error("error", Seq())

  val uploadConnector = mock[UploadConnector]
  when(uploadConnector.sendXml(any[String], any[Login], any[String])) thenReturn Future(Right(submissionId))
  when(uploadConnector.initiate(any[InitiateRequest])) thenReturn Future(Right(initiateResponse))
  when(uploadConnector.downloadFile(any[UploadConfirmation])) thenReturn Future(Right("All good"))

  val uploadConnectorF = mock[UploadConnector]
  when(uploadConnectorF.sendXml(any[String], any[Login], any[String])) thenReturn Future(Left(Error("SEND-XML-ERROR", Seq("Received exception from upstream service"))))
  when(uploadConnectorF.initiate(any[InitiateRequest])) thenReturn Future(Left(Error("INITIATE-ERROR", Seq("Received exception from upscan service"))))
  when(uploadConnectorF.downloadFile(any[UploadConfirmation])) thenReturn Future(Left(Error("UPLOAD-ERROR", Seq("Received exception from upscan service"))))

  val userReportUploadsConnectorMock = mock[UserReportUploadsConnector]
  when(userReportUploadsConnectorMock.save(any[UserReportUpload])) thenReturn Future(Right(Unit))
  when(userReportUploadsConnectorMock.getById(any[String], any[Login])) thenReturn Future(Right(Some(userReport)))

  val userReportUploadsConnectorFailMock = mock[UserReportUploadsConnector]
  when(userReportUploadsConnectorFailMock.save(any[UserReportUpload])) thenReturn Future(Left(error))
  when(userReportUploadsConnectorFailMock.getById(any[String], any[Login])) thenReturn Future(Left(error))

  val reportStatusConnectorMock = mock[ReportStatusConnector]
  when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])) thenReturn Future(Right(Unit))
  when(reportStatusConnectorMock.saveUserInfo(any[String], any[Login])) thenReturn Future(Right(Unit))

  val reportStatusConnectorFailMock = mock[ReportStatusConnector]
  when(reportStatusConnectorFailMock.save(any[ReportStatus], any[Login])) thenReturn Future(Left(error))
  when(reportStatusConnectorFailMock.saveUserInfo(any[String], any[Login])) thenReturn Future(Left(error))

  def loggedInController(
                      connector: UploadConnector,
                      userReportUploadsConnector: UserReportUploadsConnector = userReportUploadsConnectorMock,
                      reportStatusConnector: ReportStatusConnector = reportStatusConnectorMock) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    FakeDataCacheConnector.save[Login]("", LoginId.toString, login)
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, getEmptyCacheMap,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute),
      connector, userReportUploadsConnector, reportStatusConnector)
  }

  def notLoggedInController(
                             connector: UploadConnector,
                             userReportUploadsConnector: UserReportUploadsConnector = userReportUploadsConnectorMock,
                             reportStatusConnector: ReportStatusConnector = reportStatusConnectorMock) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, getEmptyCacheMap,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute),
      connector, userReportUploadsConnector, reportStatusConnector)
  }

  def viewAsString(form: Form[_] = form) = councilTaxUpload(username, frontendAppConfig, form, Some(initiateResponse))(fakeRequest, messages).toString

  "CouncilTaxUpload Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController(uploadConnector).onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController(uploadConnector).onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return valid response on the upscan confirmation endpoint" in {
      val json = Source.fromInputStream(getClass.getResourceAsStream("/valid_upscan_confirmation.json"))
        .getLines
        .mkString("\n")
      val request = FakeRequest(POST, "/councilTaxUpload/confirmation").withJsonBody(Json.parse(json))

      val result = call(loggedInController(uploadConnector).onConfirmation, request)

      status(result) mustBe NO_CONTENT
    }

    "return invalid response on the upscan confirmation endpoint when the user upload information fails" in {
      val json = Source.fromInputStream(getClass.getResourceAsStream("/valid_upscan_confirmation.json"))
        .getLines
        .mkString("\n")
      val request = FakeRequest(POST, "/councilTaxUpload/confirmation").withJsonBody(Json.parse(json))

      val result = call(loggedInController(uploadConnector, userReportUploadsConnector = userReportUploadsConnectorFailMock).onConfirmation, request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return invalid response on the upscan confirmation endpoint when the submission fails" in {
      val json = Source.fromInputStream(getClass.getResourceAsStream("/valid_upscan_confirmation.json"))
        .getLines
        .mkString("\n")
      val request = FakeRequest(POST, "/councilTaxUpload/confirmation").withJsonBody(Json.parse(json))

      val result = call(loggedInController(uploadConnector, reportStatusConnector = reportStatusConnectorFailMock).onConfirmation, request)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return NoContent when preparing the file upload" in {
      val result = loggedInController(uploadConnector).onPrepareUpload(submissionId)(fakeRequest)

      status(result) mustBe NO_CONTENT
    }

    "return invalid response when preparing for file upload fails" in {
      val result = loggedInController(uploadConnector, reportStatusConnector = reportStatusConnectorFailMock).onPrepareUpload(submissionId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}
