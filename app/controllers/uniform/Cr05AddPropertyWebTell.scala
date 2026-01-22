/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.uniform

import journey.UniformJourney.Cr05AddProperty
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import models.PropertyType
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

// TODO test
// $COVERAGE-OFF$
class Cr05AddPropertyWebTell(govUkSumaryList: GovukSummaryList) extends GenericWebTell[(Cr05AddProperty, PropertyType, Option[Int]), Html] {

  /**
    * Not edit links.
    */
  def confirmationSummary(in: Cr05AddProperty, messagess: UniformMessages[Html]): SummaryList = {
    val sum = summaryList(in, PropertyType.EXISTING, None, messagess) // Not important, links are removed
    SummaryList(sum.rows.map(x => x.copy(actions = None)), classes = "govuk-!-margin-bottom-9")
  }

  def formatUrl(journey: String, propertyType: PropertyType, index: Option[Int]) =
    index.map { x =>
      controllers.routes.UniformController.editPropertyJourney(journey, propertyType, x).url
    }.getOrElse(
      controllers.routes.UniformController.propertyJourney(journey, propertyType).url
    )

  def summaryList(in: Cr05AddProperty, propertyType: PropertyType, index: Option[Int], messages: UniformMessages[Html]): SummaryList = {
    val uprn = SummaryListRow(
      key = Key(HtmlContent(messages("UPRN.pageLabel")), "govuk-!-width-one-half"),
      value = Value(in.uprn.map(Text.apply).getOrElse(HtmlContent(messages("summary.uprn.notEntered")))),
      actions = Some(Actions(items =
        Seq(
          ActionItem(formatUrl("add-property-UPRN", propertyType, index), HtmlContent(messages("check-answers.changeLabel")))
        )
      ))
    )

    val addressContent = HtmlFormat.fill(List(
      HtmlFormat.escape(in.address.line1),
      Html("<br />"),
      HtmlFormat.escape(in.address.line2),
      Html("<br />"),
      in.address.line3.map(line3 =>
        HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />"))
        )
      ).getOrElse(Html("")),
      in.address.line4.map(line3 =>
        HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />"))
        )
      ).getOrElse(Html("")),
      HtmlFormat.escape(in.address.postcode)
    ))

    val propertyAddress = SummaryListRow(
      key = Key(HtmlContent(messages("property-address.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(addressContent)),
      actions = Some(Actions(items =
        Seq(
          ActionItem(formatUrl("add-property-property-address", propertyType, index), HtmlContent(messages("check-answers.changeLabel")))
        )
      ))
    )

    val contactDetailsContent = HtmlFormat.fill(List(
      HtmlFormat.escape(in.propertyContactDetails.firstName),
      Html("&nbsp;"),
      HtmlFormat.escape(in.propertyContactDetails.lastName),
      Html("<br />"),
      in.propertyContactDetails.email.map(email =>
        HtmlFormat.fill(
          List(HtmlFormat.escape(email), Html("<br />"))
        )
      ).getOrElse(Html("")),
      in.propertyContactDetails.phoneNumber.map(phoneNumber =>
        HtmlFormat.fill(
          List(HtmlFormat.escape(phoneNumber))
        )
      ).getOrElse(Html(""))
    ))

    val contactDetails      = SummaryListRow(
      key = Key(HtmlContent(messages("property-contact-details.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(contactDetailsContent)),
      actions = Some(Actions(items =
        Seq(
          ActionItem(formatUrl("add-property-property-contact-details", propertyType, index), HtmlContent(messages("check-answers.changeLabel")))
        )
      ))
    )
    val sameAddressQuestion = SummaryListRow(
      key = Key(HtmlContent(messages("same-contact-address.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(
        messages(if (in.sameContactAddress) "same-contact-address.same-contact-address.Yes" else "same-contact-address.same-contact-address.No")
      )),
      actions = Some(Actions(items =
        Seq(
          ActionItem(formatUrl("add-property-same-contact-address", propertyType, index), HtmlContent(messages("check-answers.changeLabel")))
        )
      ))
    )

    val contactAddress = in.contactAddress.map { contactAddress =>
      val addressContent = HtmlFormat.fill(List(
        HtmlFormat.escape(contactAddress.line1),
        Html("<br />"),
        HtmlFormat.escape(contactAddress.line2),
        Html("<br />"),
        contactAddress.line3.map(line3 =>
          HtmlFormat.fill(
            List(HtmlFormat.escape(line3), Html("<br />"))
          )
        ).getOrElse(Html("")),
        contactAddress.line4.map(line3 =>
          HtmlFormat.fill(
            List(HtmlFormat.escape(line3), Html("<br />"))
          )
        ).getOrElse(Html("")),
        HtmlFormat.escape(contactAddress.postcode)
      ))

      SummaryListRow(
        key = Key(HtmlContent(messages("contact-address.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(addressContent)),
        actions = Some(Actions(items =
          Seq(
            ActionItem(formatUrl("add-property-same-contact-address", propertyType, index), HtmlContent(messages("check-answers.changeLabel")))
          )
        ))
      )
    }

    SummaryList(
      Seq(
        Option(uprn),
        Option(propertyAddress),
        Option(contactDetails),
        Option(sameAddressQuestion),
        contactAddress
      ).flatten
    )
  }

  override def render(in: (Cr05AddProperty, PropertyType, Option[Int]), key: String, messages: UniformMessages[Html]): Html =
    govUkSumaryList(summaryList(in._1, in._2, in._3, messages))
}
// $COVERAGE-ON$
