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

import java.io.File

import connectors.{FakeDataCacheConnector, UploadConnector}
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.{LoginId, VOAAuthorisedId}
import models.{Login, NormalMode}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.councilTaxUpload

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class CouncilTaxUploadControllerSpec extends ControllerSpecBase with MockitoSugar {

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  val formProvider = new FileUploadDataFormProvider()
  val form = formProvider()

  val username = "BA0114"
  val password = "pass"
  lazy val login = Login(username, password).encrypt
  lazy val pathForValidFile = getClass.getResource("/valid.xml")

  val submissionId = "SID38273"

  val uploadConnector = mock[UploadConnector]
  when(uploadConnector.sendXml(any[FilePart[TemporaryFile]], any[Login])) thenReturn Future.successful(Success(submissionId))

  val uploadConnectorF = mock[UploadConnector]
  when(uploadConnectorF.sendXml(any[FilePart[TemporaryFile]], any[Login])) thenReturn Future.successful(Failure(new RuntimeException("Received exception from upstream service")))

  def loggedInController(connector: UploadConnector) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    FakeDataCacheConnector.save[Login]("", LoginId.toString, login)
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, getEmptyCacheMap,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute),
      connector)
  }

  def notLoggedInController(connector: UploadConnector) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, getEmptyCacheMap,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute),
      connector)
  }

  def viewAsString(form: Form[_] = form) = councilTaxUpload(username, frontendAppConfig, form)(fakeRequest, messages).toString

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

    "redirect to the next page when valid data is submitted and backend service returns a submission Id" in {
      val file = new File(pathForValidFile.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController(uploadConnector).onSubmit(NormalMode)(req)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.ConfirmationController.onPageLoad(submissionId).url)
    }

    "return a Bad Request and errors when invalid file format is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.fileType"))
      val path = getClass.getResource("/noXmlFile.txt")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "noXmlFile.txt", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController(uploadConnector).onSubmit(NormalMode)(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when no file is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.required"))

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(), badParts = Nil))

      val result = loggedInController(uploadConnector).onSubmit(NormalMode )(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when a file over 2 Mb is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.length"))
      val path = getClass.getResource("/tooLarge.xml")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "tooLarge.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController(uploadConnector).onSubmit(NormalMode )(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when an empty file is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.required"))
      val path = getClass.getResource("/empty.xml")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "empty.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController(uploadConnector).onSubmit(NormalMode )(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "throw an exception when the upload backend service call fails" in {
      val file = new File(pathForValidFile.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      intercept[Exception] {
        val result = loggedInController(uploadConnectorF).onSubmit(NormalMode )(req)
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "on submit redirect to login page if no login details can be retrieved from the user answers" in {
      val file = new File(pathForValidFile.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody(MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = notLoggedInController(uploadConnector).onSubmit(NormalMode )(req)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

  }
}
