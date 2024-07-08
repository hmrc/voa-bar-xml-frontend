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

import cats.data.NonEmptyList
import controllers.uniform.{Cr01Cr03SubmissionWebTell, Cr05AddPropertyWebTell, Cr05CommonWebTell, Cr05SubmissionBuilderWebTell}
import journey.*
import journey.UniformJourney.{Address, ContactDetails, OtherReasonWrapper}
import ltbs.uniform.*
import ltbs.uniform.common.web.*
import ltbs.uniform.interpreters.playframework.PlayInterpreter
import play.api.Logging
import play.api.libs.json.*
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukDateInput, GovukInput, GovukRadios, GovukSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Empty, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.dateinput.{DateInput, InputItem}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.{RadioItem, Radios}
import views.html.govuk.cr05SubmissionSummary

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class AutobarsInterpreter(
  results: Results,
  messagesApi: play.api.i18n.MessagesApi,
  page_chrome: views.html.govuk.pageChrome,
  govukInput: GovukInput,
  govukRadios: GovukRadios,
  govukDateInput: GovukDateInput,
  cr05SubmissionSummary: cr05SubmissionSummary
)(implicit ec: ExecutionContext
) extends PlayInterpreter[Html](results)
  with InferFormFieldProduct[Html]
  with InferFormFieldCoProduct[Html]
  with Logging {

  override def messages(request: Request[AnyContent]): UniformMessages[Html] =
    new UniformMessages[Html] {

      // Uniform can't handle when message doesn't exist.
      override def get(key: String, args: Any*): Option[Html] = Option(Html(messagesApi
        .preferred(request)(key, args)))

      override def list(key: String, args: Any*): List[Html] = Nil
    }

  override def pageChrome(
    key: List[String],
    errors: ErrorTree,
    tell: Html,
    ask: Html,
    breadcrumbs: List[String],
    request: Request[AnyContent],
    messages: UniformMessages[Html],
    fieldStats: FormFieldStats
  ): Html =
    page_chrome(key, errors, tell, ask, breadcrumbs, messages, fieldStats)(request, messagesApi.preferred(request))

  implicit val ctTaxFormWebTell: Cr01Cr03SubmissionWebTell = new Cr01Cr03SubmissionWebTell(new GovukSummaryList())

  implicit val cr05SubmissionWebTell: Cr05SubmissionBuilderWebTell = new Cr05SubmissionBuilderWebTell(cr05SubmissionSummary)

  implicit val cr05CommonWebTell: Cr05CommonWebTell = new Cr05CommonWebTell(new GovukSummaryList())

  implicit val cr05AddPropertyWebTell: Cr05AddPropertyWebTell = new Cr05AddPropertyWebTell(new GovukSummaryList())

  private def doRenderStringField(
    pageKey: List[String],
    fieldKey: List[String],
    breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    optional: Boolean,
    messages: UniformMessages[Html]
  ): Html = {
    import uk.gov.hmrc.govukfrontend.views.html.components.Input as GovInput

    val errorMessage = renderErrorMessage(pageKey, fieldKey, errors, messages)

    val fieldValue = data.get(fieldKey.tail).flatMap(_.headOption)

    logger.debug(
      s"""
         |pageKey : $pageKey
         |fieldKey: $fieldKey
         |Errors:
         |  ${errors.mkString(" \n")}
         |
         |Value:
         |  ${data.mkString(" \n")}
         |
         |""".stripMargin
    )

    val hint = if optional then
      Option(Hint(content = Text("(optional)")))
    else
      Option.empty[Hint]

    val messageKey = fieldKey :+ "label"

    govukInput(GovInput(
      id = fieldKey.mkString("_"),
      name = fieldKey.mkString("."),
      label = Label(content = HtmlContent(messages(messageKey.mkString(".")))),
      classes = "govuk-input--width-20",
      value = fieldValue,
      errorMessage = errorMessage,
      hint = hint
    ))
  }

  implicit val stringField: FormField[String, Html] = new FormField[String, Html] {

    override def decode(out: Input): Either[ErrorTree, String] = out.toStringField().toEither

    override def encode(in: String): Input = Input.one(List(in))

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html =
      doRenderStringField(pageKey, fieldKey, breadcrumbs, data, errors, false, messages)
  }

  implicit val stringOption: FormField[Option[String], Html] = new FormField[Option[String], Html] {

    override def encode(in: Option[String]): Input = Input.one(List(in.getOrElse("")))

    override def decode(out: Input): Either[ErrorTree, Option[String]] =
      out.toStringField().toEither.map(value => if (value == "") None else Option(value))

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html =
      doRenderStringField(pageKey, fieldKey, breadcrumbs, data, errors, pageKey == fieldKey, messages)
  }

  implicit val otherReasonField: FormField[OtherReasonWrapper, Html] = new FormField[OtherReasonWrapper, Html] {
    override def decode(out: Input): Either[ErrorTree, OtherReasonWrapper] = out.toStringField().toEither.map(OtherReasonWrapper.apply)
    override def encode(in: OtherReasonWrapper): Input                     = Input.one(List(in.value))

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = doRender(
      pageKey,
      fieldKey,
      breadcrumbs,
      data,
      errors,
      messages
    )

    def doRender(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {
      import uk.gov.hmrc.govukfrontend.views.html.components.Input as GovInput

      val errorMessage = errors.get(NonEmptyList.one(fieldKey))
        .orElse {
          errors.valueAtRoot.filter(_ => pageKey == fieldKey)
        }
        .map(x => x.head.prefixWith(pageKey).render[Html](messages))
        .map(x => ErrorMessage(content = HtmlContent(x)))

      val fieldValue = data.get(fieldKey.tail).flatMap(_.headOption)

      logger.debug(
        s"""
           |pageKey : $pageKey
           |fieldKey: $fieldKey
           |Errors:
           |  ${errors.mkString(" \n")}
           |
           |Value:
           |  ${data.mkString(" \n")}
           |
           |""".stripMargin
      )

      val hint = Option(Hint(content = HtmlContent(messages("other-reason.hint"))))

      govukInput(GovInput(
        id = fieldKey.mkString("_"),
        name = fieldKey.mkString("."),
        label = Label(content = Empty),
        classes = "govuk-input",
        value = fieldValue,
        errorMessage = errorMessage,
        hint = hint
      ))
    }
  }

  implicit val dateFormField: FormField[LocalDate, Html] = new FormField[LocalDate, Html] {
    import LocalDateFormFieldEncoding.*
    val innerConverter = new LocalDateFormFieldEncoding()

    override def encode(in: LocalDate): Input = innerConverter.encode(in)

    override def decode(out: Input): Either[ErrorTree, LocalDate] = innerConverter.decode(out)

    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Html = {

      val rootError = renderErrorMessage(pageKey, fieldKey, errors, messages)

      val dayClass   = "govuk-input--width-2" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "day").map(_ => " govuk-input--error")).getOrElse(""))
      val monthClass = "govuk-input--width-2" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "month").map(_ => " govuk-input--error")).getOrElse(""))
      val yearClass  = "govuk-input--width-4" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "year").map(_ => " govuk-input--error")).getOrElse(""))

      govukDateInput(DateInput(
        id = (pageKey ++ fieldKey).mkString("."),
        items = Seq(
          InputItem(name = (fieldKey ++ day).mkString("."), classes = dayClass, value = data.get(day).flatMap(_.headOption)),
          InputItem(name = (fieldKey ++ month).mkString("."), classes = monthClass, value = data.get(month).flatMap(_.headOption)),
          InputItem(name = (fieldKey ++ year).mkString("."), classes = yearClass, value = data.get(year).flatMap(_.headOption))
        ).map(x => x.copy(label = Option(messages(x.name).toString()))),
        hint = Option(Hint(content = HtmlContent(messages(pageKey.mkString(".") + ".hint")))),
        errorMessage = rootError
      ))
    }
  }

  override def renderProduct[A](
    pageKey: List[String],
    fieldKey: List[String],
    path: _root_.ltbs.uniform.common.web.Breadcrumbs,
    values: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    pfl: ProductFieldList[A, Html]
  ): Html = {

    val htmlList = pfl.inner.map { case (subFieldId, f) =>
      f(pageKey, fieldKey :+ subFieldId, path, values, errors, messages)
    }
    HtmlFormat.fill(htmlList)
  }

  override def renderCoproduct[A](
    pageKey: List[String],
    fieldKey: List[String],
    path: _root_.ltbs.uniform.common.web.Breadcrumbs,
    values: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    cfl: CoproductFieldList[A, Html]
  ): Html = {

    val value           = values.valueAtRoot.map(_.mkString)
    val coproductValues = cfl.inner.map(_._1).toSet
    val items           = if coproductValues == NoPlanningReferenceType.order.toSet then
      NoPlanningReferenceType.order
    else if coproductValues == ReasonReportType.order.toSet then
      ReasonReportType.order
    else if coproductValues == RemovalReasonType.order.toSet then
      RemovalReasonType.order
    else if coproductValues == Set("Yes", "No") then
      List("Yes", "No")
    else
      cfl.inner.map(_._1)

    val radiosItems = items.map { name =>
      RadioItem(
        id = Option(name),
        value = Option(name),
        content = HtmlContent(messages(s"${pageKey.mkString}${fieldKey.mkString(".", ".", ".")}$name")),
        checked = value.contains(name)
      )
    }

    val errorMessage = renderErrorMessage(pageKey, fieldKey, errors, messages)

    govukRadios(
      Radios(
        idPrefix = Some(fieldKey.mkString("_")),
        items = radiosItems,
        name = fieldKey.head,
        errorMessage = errorMessage,
        classes = if coproductValues.sizeIs == 2 then "govuk-radios--inline" else ""
      )
    )
  }

  private def renderErrorMessage[A](
    pageKey: List[String],
    fieldKey: List[String],
    errors: ErrorTree,
    messages: UniformMessages[Html]
  ): Option[ErrorMessage] =
    errors.get(NonEmptyList.one(fieldKey))
      .orElse {
        errors.valueAtRoot.filter(_ => pageKey == fieldKey)
      }
      .map { errorMsgNel =>
        val html = errorMsgNel.head.prefixWith(pageKey).render[Html](messages)
        ErrorMessage(content = HtmlContent(html))
      }

  private def genericCoproductFieldList[A](
    values: List[String]
  )(implicit rds: Reads[A],
    tjs: Writes[A]
  ): CoproductFieldList[A, Html] = new CoproductFieldList[A, Html] {

    override def decode(in: Input): Either[ErrorTree, A] =
      in.toStringField().toEither.flatMap(str =>
        JsString(str).validate[A].asEither.left.map {
          e =>
            logger.warn(s"Decode coproduct $in failed. Errors: $e")
            ErrorMsg("required").toTree
        }
      )

    override def encode(a: A): Input = Input.one(
      List(Json.stringify(Json.toJson[A](a)))
    )

    override val inner = values.map(_ -> stringField.render)

    def stats = FormFieldStats()
  }

  private def readStringOrOptionString(jsValue: JsValue): String =
    jsValue match {
      case JsString(str) => java.net.URLEncoder.encode(str, "UTF-8")
      case _             => ""
    }

  private def createProductFieldList[A](
    innerParams: List[(String, (List[String], List[String], Breadcrumbs, Input, ErrorTree, UniformMessages[Html]) => Html)]
  )(implicit rds: Reads[A],
    tjs: Writes[A]
  ): ProductFieldList[A, Html] =
    new ProductFieldList[A, Html] {

      override def decode(in: Input): Either[ErrorTree, A] = {
        val jsObject = JsObject(
          in.map((key, value) => value.headOption.flatMap(v => Some(key.mkString(".") -> JsString(v)))).flatten.toSeq
        )

        jsObject.validate[A].asEither.left.map {
          e =>
            logger.warn(s"Decode product $in failed. Errors: $e")
            ErrorMsg("required").toTree
        }
      }

      override def encode(a: A): Input =
        Json.toJson[A](a) match {
          case jsObject: JsObject =>
            val encodedString = jsObject.fields.map((key, json) => s"$key=${readStringOrOptionString(json)}").mkString("&")
            Input.fromUrlEncodedString(encodedString).getOrElse(Input.empty)
          case _                  => Input.empty
        }

      override val inner = innerParams

      def stats = FormFieldStats()
    }

  implicit def cflReasonReportType: CoproductFieldList[ReasonReportType, Html]               = genericCoproductFieldList(ReasonReportType.order)
  implicit def cflRemovalReasonType: CoproductFieldList[RemovalReasonType, Html]             = genericCoproductFieldList(RemovalReasonType.order)
  implicit def cflNoPlanningReferenceType: CoproductFieldList[NoPlanningReferenceType, Html] = genericCoproductFieldList(NoPlanningReferenceType.order)
  implicit def cflYesNoType: CoproductFieldList[YesNoType, Html]                             = genericCoproductFieldList(List("Yes", "No"))

  implicit def pflContactDetails: ProductFieldList[ContactDetails, Html] = createProductFieldList(
    List(
      "firstName"   -> stringField.render,
      "lastName"    -> stringField.render,
      "email"       -> stringOption.render,
      "phoneNumber" -> stringOption.render
    )
  )

  implicit def pflAddress: ProductFieldList[Address, Html] = createProductFieldList(
    List(
      "line1"    -> stringField.render,
      "line2"    -> stringField.render,
      "line3"    -> stringOption.render,
      "line4"    -> stringOption.render,
      "postcode" -> stringField.render
    )
  )

}
