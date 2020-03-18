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

import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import ltbs.uniform.common.web.{CoproductFieldList, FormField, FormFieldStats, InferFormFieldCoProduct, InferFormFieldProduct, InferListingPages, ProductFieldList, WebMonad, WebMonadConstructor}
import ltbs.uniform.interpreters.playframework.PlayInterpreter
import ltbs.uniform.validation.Rule
import ltbs.uniform._
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.govukInput
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.errorsummary.{ErrorLink, ErrorSummary}
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label

import scala.concurrent.ExecutionContext

class AutobarsInterpreter (
                           results: Results,
                           messagesApi: play.api.i18n.MessagesApi,
                           page_chrome: views.html.govuk.pageChrome,
                             govukInput: govukInput
                         )(implicit ec: ExecutionContext) extends PlayInterpreter[Html](results)
  with InferFormFieldProduct[Html]
  with InferFormFieldCoProduct[Html]
  with InferListingPages[Html] {

  override def blankTell: Html = Html("")

  override def messages(request: Request[AnyContent]): UniformMessages[Html] = {
    new UniformMessages[Html] {

      //Uniform can't handle when message doesn't exist.
      override def get(key: String, args: Any*): Option[Html] = Option(Html(messagesApi
        .preferred(request)(key, args)))

      override def list(key: String, args: Any*): List[Html] = Nil
    }
  }

  override def pageChrome(
                           key: List[String],
                           errors: ErrorTree,
                           tell: Html,
                           ask: Html,
                           breadcrumbs: List[String],
                           request: Request[AnyContent],
                           messages: UniformMessages[Html],
                           fieldStats: FormFieldStats): Html = {

    page_chrome(
      key, errors, tell, ask, breadcrumbs, messages, fieldStats)(request, messagesApi.preferred(request))
  }

  implicit val webTellLong = new WebTell[Long] {
    override def render(in: Long, key: String, messages: UniformMessages[Html]): Html = Html(s"in: ${in}, key:${messages}")
  }

   implicit val stringField = new FormField[String, Html] {
      override def decode(out: Input): Either[ErrorTree, String] = out.toStringField().toEither
      override def encode(in: String): Input = Input.one(List(in))


      override def render(pageKey: List[String],
                          fieldKey: List[String],
                          breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
                          data: Input, errors: ErrorTree,
                          messages: UniformMessages[Html]): Html =  {
        import uk.gov.hmrc.govukfrontend.views.html.components.{Input => GovInput}

        val errorMessage = errors.valueAtRoot
          .map(x => x.head.render[Html](messages))
          .map(x => ErrorMessage(content = HtmlContent(x)))

        val messageKey = ("label" :: fieldKey ++ pageKey).reverse

        govukInput(GovInput(
          id = fieldKey.mkString("_"),
          name = fieldKey.mkString("."),
          label = Label(content = HtmlContent(messages(messageKey.mkString(".")))),
          classes="govuk-input--width-20",
          value = data.valueAtRoot.flatMap(_.headOption),
          errorMessage = errorMessage
        ))
      }
  }


  override def renderProduct[A](pageKey: List[String],
                                fieldKey: List[String],
                                path: _root_.ltbs.uniform.common.web.Breadcrumbs,
                                values: Input,
                                errors: ErrorTree,
                                messages: UniformMessages[Html],
                                pfl: ProductFieldList[A, Html]): Html = {

    val htmlList = pfl.inner.map { case (subFieldId, f) =>
      f(pageKey, fieldKey :+ subFieldId, path, values, errors, messages)
    }
    HtmlFormat.fill(htmlList)
  }

  override def renderCoproduct[A](pageKey: List[String],
                                  fieldKey: List[String],
                                  path: _root_.ltbs.uniform.common.web.Breadcrumbs,
                                  values: Input,
                                  errors: ErrorTree,
                                  messages: UniformMessages[Html],
                                  cfl: CoproductFieldList[A, Html]): Html = Html("should render coproduct :👻")
}
