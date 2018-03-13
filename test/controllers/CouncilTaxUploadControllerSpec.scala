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
import connectors.FakeDataCacheConnector
import controllers.actions._
import forms.FileUploadDataFormProvider
import identifiers.VOAAuthorisedId
import models.NormalMode
import play.api.data.Form
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.Helpers._
import utils.FakeNavigator
import views.html.councilTaxUpload

class CouncilTaxUploadControllerSpec extends ControllerSpecBase {

  val username = "BA0114"

  def onwardRoute = routes.LoginController.onPageLoad(NormalMode)

  val formProvider = new FileUploadDataFormProvider()
  val form = formProvider()

  def loggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    FakeDataCacheConnector.save[String]("", VOAAuthorisedId.toString, username)
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider,new FakeNavigator(desiredRoute = onwardRoute))
  }

  def notLoggedInController(dataRetrievalAction: DataRetrievalAction = getEmptyCacheMap) = {
    FakeDataCacheConnector.resetCaptures()
    new CouncilTaxUploadController(frontendAppConfig, messagesApi, dataRetrievalAction,
      new DataRequiredActionImpl, FakeDataCacheConnector, formProvider, new FakeNavigator(desiredRoute = onwardRoute))
  }

  def viewAsString(form: Form[_] = form) = councilTaxUpload(username, frontendAppConfig, form)(fakeRequest, messages).toString

  "CouncilTaxUpload Controller" must {

    "return OK and the correct view for a GET" in {
      val result = loggedInController().onPageLoad(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "if not authorized by VOA must go to the login page" in {
      val result = notLoggedInController().onPageLoad()(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "redirect to the next page when valid data is submitted" in {
      val path = getClass.getResource("/valid.xml")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "valid.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody( MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController().onSubmit(NormalMode, username)(req)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors when invalid file format is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.fileType"))
      val path = getClass.getResource("/noXmlFile.txt")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "noXmlFile.txt", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody( MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController().onSubmit(NormalMode, username)(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when no file is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.required"))

      val req = fakeRequest.withBody( MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(), badParts = Nil))

      val result = loggedInController().onSubmit(NormalMode, username)(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when a file over 2 Mb is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.length"))
      val path = getClass.getResource("/tooLarge.xml")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "tooLarge.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody( MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController().onSubmit(NormalMode, username)(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

    "return a Bad Request and errors when an empty file is submitted" in {
      val boundForm = form.withGlobalError(messages("councilTaxUpload.error.xml.required"))
      val path = getClass.getResource("/empty.xml")
      val file = new File(path.getPath)
      val tempFile = new TemporaryFile(file)

      val part = FilePart[TemporaryFile](key = "xml", filename = "empty.xml", contentType = None, ref = tempFile)

      val req = fakeRequest.withBody( MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(part), badParts = Nil))

      val result = loggedInController().onSubmit(NormalMode, username)(req)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe viewAsString(boundForm)
    }

  }
}
