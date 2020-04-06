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

import cats.data.NonEmptyList
import journey.UniformJourney.CtTaxForm
import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import ltbs.uniform.common.web.{CoproductFieldList, FormField, FormFieldStats, InferFormFieldCoProduct, InferFormFieldProduct, InferListingPages, ProductFieldList, WebMonad, WebMonadConstructor}
import ltbs.uniform.interpreters.playframework.PlayInterpreter
import ltbs.uniform._
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{govukInput, govukSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Value}

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

  val logger = Logger(this.getClass)

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

  implicit val ctTaxFormWebTell = new WebTell[CtTaxForm] {
    val sumaryList = new govukSummaryList()

    override def render(in: CtTaxForm, key: String, messages: UniformMessages[Html]): Html = {

      val baReport = SummaryListRow(
        key = Key(HtmlContent(messages("ba-report.pageLabel"))),
        value = Value(Text(in.baReport)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("ba-report").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
      val baRef = SummaryListRow(
        key = Key(HtmlContent(messages("ba-ref.pageLabel"))),
        value = Value(Text(in.baRef)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("ba-ref").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )

      val addressContent = HtmlFormat.fill(List(
      HtmlFormat.escape(in.address.line1),Html("<br />"),
      HtmlFormat.escape(in.address.line2),Html("<br />"),
      in.address.line3.map(line3 => HtmlFormat.fill(
        List(HtmlFormat.escape(line3), Html("<br />")))
      ).getOrElse(Html("")),
      in.address.line4.map(line3 => HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />")))
        ).getOrElse(Html("")),
        HtmlFormat.escape(in.address.postcode)
      ))

      val propertyAddress = SummaryListRow(
        key = Key(HtmlContent(messages("property-address.pageLabel"))),
        value = Value(HtmlContent(addressContent)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("property-address").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )

      val contactDetailsContent = HtmlFormat.fill(List(
        HtmlFormat.escape(in.propertyContactDetails.firstName),Html("<br />"),
        HtmlFormat.escape(in.propertyContactDetails.lastName),Html("<br />"),
        in.propertyContactDetails.email.map(email => HtmlFormat.fill(
          List(HtmlFormat.escape(email), Html("<br />"))
        )).getOrElse(Html("")),
        in.propertyContactDetails.phoneNumber.map(phoneNumber => HtmlFormat.fill(
          List(HtmlFormat.escape(phoneNumber))
        )).getOrElse(Html(""))
      ))

      val contactDetails = SummaryListRow(
        key = Key(HtmlContent(messages("property-contact-details.pageLabel"))),
        value = Value(HtmlContent(contactDetailsContent)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("property-contact-details").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
      sumaryList(SummaryList(Seq(baReport, baRef, propertyAddress, contactDetails)))
    }
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

        val errorMessage = errors.get(NonEmptyList.one(fieldKey))
          .orElse {
            errors.valueAtRoot.filter(_ => pageKey == fieldKey)
          }
          .map(x => x.head.render[Html](messages))
          .map(x => ErrorMessage(content = HtmlContent(x)))

        val fieldValue = data.get(fieldKey.tail).flatMap(_.headOption)

        logger.debug(
          s"""
            |pageKey : ${pageKey}
            |fieldKey: ${fieldKey}
            |Errors:
            |  ${errors.mkString(" \n")}
            |
            |Value:
            |  ${data.mkString(" \n")}
            |
            |""".stripMargin)


        val messageKey = fieldKey :+ "label"

        govukInput(GovInput(
          id = fieldKey.mkString("_"),
          name = fieldKey.mkString("."),
          label = Label(content = HtmlContent(messages(messageKey.mkString(".")))),
          classes="govuk-input--width-20",
          value = fieldValue,
          errorMessage = errorMessage
        ))
      }
  }

  implicit val stringOption = new FormField[Option[String], Html] {

    override def encode(in: Option[String]): Input = Input.one(List(in.getOrElse("")))

    override def decode(out: Input): Either[ErrorTree, Option[String]] = {
      out.toStringField().toEither.map(value => if(value == "") None else Option(value))
    }

    override def render(pageKey: List[String],
                        fieldKey: List[String],
                        breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
                        data: Input, errors: ErrorTree, messages: UniformMessages[Html]): Html = {
      stringField.render(pageKey, fieldKey, breadcrumbs, data, errors, messages)
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
                                  cfl: CoproductFieldList[A, Html]): Html = {
    Html(
      s"should render coproduct<br /> stats: ${cfl.stats}, <br />${cfl.inner} <br />")
  }
}
