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

package controllers.uniform


import java.time.format.DateTimeFormatter

import journey.UniformJourney.{Cr01Cr03Submission, Cr05Submission}
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Value}
import views.html.govuk.cr05SubmissionConfirmation

// TODO test
// $COVERAGE-OFF$
class Cr05SubmissionWebTell(cr05SubmissionConfirmation: cr05SubmissionConfirmation) extends GenericWebTell[Cr05Submission, Html] {

  def confirmationSummary(in: Cr01Cr03Submission, messages: UniformMessages[Html]): SummaryList = {
    val sum = summaryList(in, messages)
    SummaryList(sum.rows.map(x => x.copy(actions = None)), "govuk-!-margin-bottom-9")
  }

  def summaryList(in: Cr01Cr03Submission, messages: UniformMessages[Html]): SummaryList = {

    val baReport = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-ba-report.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baReport)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-ba-report").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )
    val baRef = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-ba-ref.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baRef)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-ba-ref").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val uprn = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-UPRN.pageLabel")), "govuk-!-width-one-half"),
      value = Value(in.uprn.map(Text).getOrElse(HtmlContent(messages("summary.uprn.notEntered")))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-UPRN").url,
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
      key = Key(HtmlContent(messages("add-property-property-address.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(addressContent)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-property-address").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val contactDetailsContent = HtmlFormat.fill(List(
      HtmlFormat.escape(in.propertyContactDetails.firstName),Html("&nbsp;"),
      HtmlFormat.escape(in.propertyContactDetails.lastName),Html("<br />"),
      in.propertyContactDetails.email.map(email => HtmlFormat.fill(
        List(HtmlFormat.escape(email), Html("<br />"))
      )).getOrElse(Html("")),
      in.propertyContactDetails.phoneNumber.map(phoneNumber => HtmlFormat.fill(
        List(HtmlFormat.escape(phoneNumber))
      )).getOrElse(Html(""))
    ))

    val contactDetails = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-property-contact-details.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(contactDetailsContent)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-property-contact-details").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )
    val sameAddressQuestion = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-same-contact-address.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(
        messages(if(in.sameContactAddress)"add-property-same-contact-address.add-property-same-contact-address.Yes" else "add-property-same-contact-address.add-property-same-contact-address.No")
      )),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-same-contact-address").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val contactAddress = in.contactAddress.map { contactAddress =>
      val addressContent = HtmlFormat.fill(List(
        HtmlFormat.escape(contactAddress.line1),Html("<br />"),
        HtmlFormat.escape(contactAddress.line2),Html("<br />"),
        contactAddress.line3.map(line3 => HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />")))
        ).getOrElse(Html("")),
        contactAddress.line4.map(line3 => HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />")))
        ).getOrElse(Html("")),
        HtmlFormat.escape(contactAddress.postcode)
      ))

      SummaryListRow(
        key = Key(HtmlContent(messages("add-property-contact-address.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(addressContent)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-contact-address").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    val effectiveDate = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-effective-date.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(formatter.format(in.effectiveDate))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-effective-date").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val havePlanningRef = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-have-planning-ref.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(if(in.havePlaningReference) messages("add-property-have-planning-ref.add-property-have-planning-ref.Yes") else messages("add-property-have-planning-ref.add-property-have-planning-ref.No"))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-have-planning-ref").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val planningRef = in.planningRef.map { planningRef =>
      SummaryListRow(
        key = Key(HtmlContent(messages("add-property-have-planning-ref.pageLabel")), "govuk-!-width-one-half"),
        value = Value(Text(planningRef)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-have-planning-ref").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val noPlanningRef = in.noPlanningReference.map { noPlanningRef =>
      SummaryListRow(
        key = Key(HtmlContent(messages("add-property-why-no-planning-ref.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(messages("add-property-why-no-planning-ref.add-property-why-no-planning-ref." + noPlanningRef.getClass.getSimpleName.replace("$","")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-why-no-planning-ref").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val comments =
      SummaryListRow(
        key = Key(HtmlContent(messages("add-property-comments.pageLabel")), "govuk-!-width-one-half"),
        value = Value(in.comments.map(Text).getOrElse(HtmlContent(messages("summary.comments.none")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.addPropertyJourney("add-property-comments").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )


    SummaryList(
//      reasonSummaryList(in, messages) ++
        Seq(
          Option(baReport),
          Option(baRef),
          Option(uprn),
          Option(propertyAddress),
          Option(contactDetails),
          Option(sameAddressQuestion),
          contactAddress,
          Option(effectiveDate),
          Option(havePlanningRef),
          planningRef,
          noPlanningRef,
          Option(comments)
        ).flatten)
  }

  override def render(in: Cr05Submission, key: String, messages: UniformMessages[Html]): Html =
    cr05SubmissionConfirmation(in.splitProperties.head, in.mergeProperties.toList, messages)
}
// $COVERAGE-ON$