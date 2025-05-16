/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{FakeDataCacheConnector, ReportStatusConnector, UploadConnector, UserReportUploadsConnector}
import controllers.actions.*
import forms.FileUploadDataFormProvider
import identifiers.{LoginId, VOAAuthorisedId}
import models.*
import models.UpScanRequests.{InitiateRequest, InitiateResponse, UploadRequest}
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeNavigator
import views.ViewSpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

class CouncilTaxUploadControllerSpec extends ControllerSpecBase with ViewSpecBase with MockitoSugar {

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  def ec                   = app.injector.instanceOf[ExecutionContext]
  def controllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def councilTaxUpload = app.injector.instanceOf[views.html.councilTaxUpload]
  def errorTemplate    = app.injector.instanceOf[views.html.error_template]

  def configuration = app.injector.instanceOf[Configuration]

  val formProvider                      = new FileUploadDataFormProvider()
  val form                              = formProvider()
  implicit val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

  val username              = "BA0114"
  val password              = "pass"
  lazy val login            = Login(username, password).encrypt(configuration)
  lazy val pathForValidFile = getClass.getResource("/valid.xml")

  val submissionId = "SID38273"
  val reference    = submissionId
  val uploadUrl    = "http://foo.bar"
  val userReport   = UserReportUpload(reference, username, password)

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
  val error            = Error("error", Seq())

  val uploadConnector = mock[UploadConnector]
  when(uploadConnector.sendXml(any[String], any[Login], any[String])(using any[HeaderCarrier])).thenReturn(Future(Right(submissionId)))
  when(uploadConnector.initiate(any[InitiateRequest])(using any[HeaderCarrier])).thenReturn(Future(Right(initiateResponse)))

  val uploadConnectorF = mock[UploadConnector]
  when(uploadConnectorF.sendXml(any[String], any[Login], any[String])(using any[HeaderCarrier])).thenReturn(Future(Left(Error(
    "SEND-XML-ERROR",
    Seq("Received exception from upstream service")
  ))))
  when(uploadConnectorF.initiate(any[InitiateRequest])(using any[HeaderCarrier])).thenReturn(Future(Left(Error(
    "INITIATE-ERROR",
    Seq("Received exception from upscan service")
  ))))

  val userReportUploadsConnectorMock = mock[UserReportUploadsConnector]
  when(userReportUploadsConnectorMock.save(any[UserReportUpload])(using any[HeaderCarrier])).thenReturn(Future(Right(())))
  when(userReportUploadsConnectorMock.getById(any[String], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Right(Some(userReport))))

  val userReportUploadsConnectorFailMock = mock[UserReportUploadsConnector]
  when(userReportUploadsConnectorFailMock.save(any[UserReportUpload])(using any[HeaderCarrier])).thenReturn(Future(Left(error)))
  when(userReportUploadsConnectorFailMock.getById(any[String], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Left(error)))

  val reportStatusConnectorMock = mock[ReportStatusConnector]
  when(reportStatusConnectorMock.save(any[ReportStatus], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Right(())))
  when(reportStatusConnectorMock.saveUserInfo(any[String], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Right(())))

  val reportStatusConnectorFailMock = mock[ReportStatusConnector]
  when(reportStatusConnectorFailMock.save(any[ReportStatus], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Left(error)))
  when(reportStatusConnectorFailMock.saveUserInfo(any[String], any[Login])(using any[HeaderCarrier])).thenReturn(Future(Left(error)))

  def loggedInController(
    connector: UploadConnector,
    userReportUploadsConnector: UserReportUploadsConnector = userReportUploadsConnectorMock,
    reportStatusConnector: ReportStatusConnector = reportStatusConnectorMock
  ) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    FakeDataCacheConnector.save[Login]("", LoginId.toString, login)
    new CouncilTaxUploadController(
      configuration,
      frontendAppConfig,
      messagesApi,
      getEmptyCacheMap,
      new DataRequiredActionImpl(ec),
      FakeDataCacheConnector,
      formProvider,
      new FakeNavigator(desiredRoute = onwardRoute),
      connector,
      councilTaxUpload,
      errorTemplate,
      userReportUploadsConnector,
      reportStatusConnector,
      controllerComponents
    )
  }

  def notLoggedInController(
    connector: UploadConnector,
    userReportUploadsConnector: UserReportUploadsConnector = userReportUploadsConnectorMock,
    reportStatusConnector: ReportStatusConnector = reportStatusConnectorMock
  ) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxUploadController(
      configuration,
      frontendAppConfig,
      messagesApi,
      getEmptyCacheMap,
      new DataRequiredActionImpl(ec),
      FakeDataCacheConnector,
      formProvider,
      new FakeNavigator(desiredRoute = onwardRoute),
      connector,
      councilTaxUpload,
      errorTemplate,
      userReportUploadsConnector,
      reportStatusConnector,
      controllerComponents
    )
  }

  def viewAsString(form: Form[?] = form) = councilTaxUpload(username, form, Some(initiateResponse))(using fakeRequest, messages).toString

  "CouncilTaxUpload Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController(uploadConnector).onPageLoad(false)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController(uploadConnector).onPageLoad(false)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return NoContent when preparing the file upload" in {
      val result = loggedInController(uploadConnector).onPrepareUpload(submissionId)(fakeRequest)

      status(result) mustBe NO_CONTENT
    }

    "return invalid response when preparing for file upload fails" in {
      val result = loggedInController(uploadConnector, reportStatusConnector = reportStatusConnectorFailMock).onPrepareUpload(submissionId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return valid response when saving upload error status" in {
      val json    = Source.fromInputStream(getClass.getResourceAsStream("/validError.json"))
        .getLines()
        .mkString("\n")
      val request = FakeRequest(POST, s"/councilTaxUpload/error/$submissionId").withJsonBody(Json.parse(json))

      val result = call(loggedInController(uploadConnector).onError(submissionId), request)

      status(result) mustBe NO_CONTENT
    }

    "return invalid response when saving upload error status fails" in {
      val json    = Source.fromInputStream(getClass.getResourceAsStream("/validError.json"))
        .getLines()
        .mkString("\n")
      val request = FakeRequest(POST, s"/councilTaxUpload/error/$submissionId").withJsonBody(Json.parse(json))

      val result = call(
        loggedInController(
          uploadConnector,
          userReportUploadsConnector = userReportUploadsConnectorFailMock,
          reportStatusConnector = reportStatusConnectorFailMock
        ).onError(submissionId),
        request
      )

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

  }
}
