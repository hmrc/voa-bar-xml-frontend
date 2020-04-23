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

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

import cats.data.NonEmptyList
import journey.UniformJourney.CtTaxForm
import journey.{LocalDateFormFieldEncoding, No, NoPlanningReferenceType, Yes, YesNoType}
import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import ltbs.uniform.common.web.{CoproductFieldList, FormField, FormFieldEncoding, FormFieldStats, InferFormFieldCoProduct, InferFormFieldProduct, InferListingPages, ProductFieldList, WebMonad, WebMonadConstructor}
import ltbs.uniform.interpreters.playframework.PlayInterpreter
import ltbs.uniform._
import play.api.Logger
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{govukDateInput, govukInput, govukRadios, govukSummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.dateinput.{DateInput, InputItem}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.{RadioItem, Radios}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Value}

import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import scala.util.Try

class AutobarsInterpreter (
                           results: Results,
                           messagesApi: play.api.i18n.MessagesApi,
                           page_chrome: views.html.govuk.pageChrome,
                           govukInput: govukInput,
                           govukRadios: govukRadios,
                           govukDateInput: govukDateInput
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

      val uprn = in.uprn.map { uprn =>
        SummaryListRow(
          key = Key(HtmlContent(messages("UPRN.pageLabel"))),
          value = Value(Text(uprn)),
          actions = Some(Actions(items = Seq(
            ActionItem(controllers.routes.UniformController.myJourney("UPRN").url,
              HtmlContent(messages("check-answers.changeLabel"))))
          ))
        )
      }

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
      val sameAddressQuestion = SummaryListRow(
        key = Key(HtmlContent(messages("same-contact-address.pageLabel"))),
        value = Value(HtmlContent(
          messages(if(in.sameContactAddress)"same-contact-address.same-contact-address.Yes" else "same-contact-address.same-contact-address.No")
        )),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("same-contact-address").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )

      val contactAddress = in.contactAddress.map { contactAddress =>
        val addressContent = HtmlFormat.fill(List(
          HtmlFormat.escape(contactAddress.line1),Html("<br />"),
          HtmlFormat.escape(contactAddress.line2),Html("<br />"),
          in.address.line3.map(line3 => HtmlFormat.fill(
            List(HtmlFormat.escape(line3), Html("<br />")))
          ).getOrElse(Html("")),
          in.address.line4.map(line3 => HtmlFormat.fill(
            List(HtmlFormat.escape(line3), Html("<br />")))
          ).getOrElse(Html("")),
          HtmlFormat.escape(contactAddress.postcode)
        ))

        SummaryListRow(
          key = Key(HtmlContent(messages("contact-address.pageLabel"))),
          value = Value(HtmlContent(addressContent)),
          actions = Some(Actions(items = Seq(
            ActionItem(controllers.routes.UniformController.myJourney("contact-address").url,
              HtmlContent(messages("check-answers.changeLabel"))))
          ))
        )
      }

      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val effectiveDate = SummaryListRow(
        key = Key(HtmlContent(messages("effective-date.pageLabel"))),
        value = Value(Text(formatter.format(in.effectiveDate))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("effective-date").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )

      val planningRef = in.planningRef.map { planningRef =>
        SummaryListRow(
          key = Key(HtmlContent(messages("planning-ref.pageLabel"))),
          value = Value(Text(planningRef)),
          actions = Some(Actions(items = Seq(
            ActionItem(controllers.routes.UniformController.myJourney("planning-ref").url,
              HtmlContent(messages("check-answers.changeLabel"))))
          ))
        )
      }

      val noPlanningRef = in.noPlanningReference.map { noPlanningRef =>
        SummaryListRow(
          key = Key(HtmlContent(messages("why-no-planning-ref.pageLabel"))),
          value = Value(HtmlContent(messages("why-no-planning-ref.why-no-planning-ref." + noPlanningRef.getClass.getSimpleName.replace("$","")))),
          actions = Some(Actions(items = Seq(
            ActionItem(controllers.routes.UniformController.myJourney("why-no-planning-ref").url,
              HtmlContent(messages("check-answers.changeLabel"))))
          ))
        )
      }

      val comments = in.comments.map { comment =>
        SummaryListRow(
          key = Key(HtmlContent(messages("comments.pageLabel"))),
          value = Value(Text(comment)),
          actions = Some(Actions(items = Seq(
            ActionItem(controllers.routes.UniformController.myJourney("comments").url,
              HtmlContent(messages("check-answers.changeLabel"))))
          ))
        )
      }

      sumaryList(SummaryList(Seq(
        Option(baReport),
        Option(baRef),
        uprn,
        Option(propertyAddress),
        Option(contactDetails),
        Option(sameAddressQuestion),
        contactAddress,
        Option(effectiveDate),
        planningRef,
        noPlanningRef,
        comments
      ).flatten))
    }
  }


  implicit val stringField = new FormField[String, Html] {
      override def decode(out: Input): Either[ErrorTree, String] = out.toStringField().toEither
      override def encode(in: String): Input = Input.one(List(in))

      override def render(pageKey: List[String],
                          fieldKey: List[String],
                          breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
                          data: Input, errors: ErrorTree,
                          messages: UniformMessages[Html]): Html = _render(
        pageKey, fieldKey, breadcrumbs, data, errors, false, messages)


        def _render(pageKey: List[String],
                            fieldKey: List[String],
                            breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
                            data: Input, errors: ErrorTree,
                            optional: Boolean,
                            messages: UniformMessages[Html]): Html =  {
        import uk.gov.hmrc.govukfrontend.views.html.components.{Input => GovInput}

        val errorMessage = errors.get(NonEmptyList.one(fieldKey))
          .orElse {
            errors.valueAtRoot.filter(_ => pageKey == fieldKey)
          }
          .map(x => x.head.prefixWith(pageKey).render[Html](messages))
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

        val hint = if(optional) {
          Option(Hint(content = Text("Optional")))
        }else {
          Option.empty[Hint]
        }

        val messageKey = fieldKey :+ "label"

        govukInput(GovInput(
          id = fieldKey.mkString("_"),
          name = fieldKey.mkString("."),
          label = Label(content = HtmlContent(messages(messageKey.mkString(".")))),
          classes="govuk-input--width-20",
          value = fieldValue,
          errorMessage = errorMessage,
          hint = hint
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
      stringField._render(pageKey, fieldKey, breadcrumbs, data, errors, pageKey == fieldKey, messages)
    }
  }

  implicit val dateFormField = new FormField[LocalDate, Html] {
    import LocalDateFormFieldEncoding._
    val innerConverter = new LocalDateFormFieldEncoding()

    override def encode(in: LocalDate): Input = innerConverter.encode(in)

    override def decode(out: Input): Either[ErrorTree, LocalDate] = innerConverter.decode(out)


    override def render(pageKey: List[String], fieldKey: List[String],
                        breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
                        data: Input, errors: ErrorTree,
                        messages: UniformMessages[Html]): Html = {

      val rootError = errors.get(NonEmptyList.one(fieldKey))
        .orElse {
          errors.valueAtRoot.filter(_ => pageKey == fieldKey)
        }
        .map(x => x.head.prefixWith(pageKey).render[Html](messages))
        .map(x => ErrorMessage(content = HtmlContent(x)))

      val dayClass = "govuk-input--width-2" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "day").map(_ => " govuk-input--error")).getOrElse(""))
      val monthClass = "govuk-input--width-2" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "month").map(_ => " govuk-input--error")).getOrElse(""))
      val yearClass = "govuk-input--width-4" + (errors.valueAtRoot.flatMap(x => x.find(_.msg == "year").map(_ => " govuk-input--error")).getOrElse(""))

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

    val value = values.valueAtRoot.map(_.mkString)
    val coproductValues = cfl.inner.map(_._1).toSet
    val items = if(coproductValues == NoPlanningReferenceType.order.toSet) {
      NoPlanningReferenceType.order
    } else if (coproductValues == Set("Yes", "No")) {
      List("Yes", "No")
    } else {
      cfl.inner.map(_._1)
    }

    val radiosItems = items.map { name:String =>
      RadioItem(
        id=Option(name),
        value=Option(name),
        content = HtmlContent(messages(s"${pageKey.mkString}${fieldKey.mkString(".", ".", ".")}${name}")),
        checked = value.map(_ == name).getOrElse(false)
      )
    }

    govukRadios(Radios(items = radiosItems, name = fieldKey.head))
  }
}
