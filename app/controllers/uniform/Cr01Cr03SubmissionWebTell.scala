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

import journey.UniformJourney.Cr01Cr03Submission
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.govukSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Value}

class Cr01Cr03SubmissionWebTell(govUkSumaryList: govukSummaryList) extends GenericWebTell[Cr01Cr03Submission, Html] {

  def confirmationSummary(in: Cr01Cr03Submission, messages: Messages): SummaryList = {
    val sum = summaryList(in, new UniformMessages[Html] {
      override def get(key: String, args: Any*): Option[Html] = Option(Html(messages(key, args)))

      override def list(key: String, args: Any*): List[Html] = Nil
    })
    SummaryList(sum.rows.map(x => x.copy(actions = None)), "govuk-!-margin-bottom-9")
  }

  private def reasonSummaryList(in: Cr01Cr03Submission, messages: UniformMessages[Html]): Seq[SummaryListRow] = {
    val reasonReport =
      SummaryListRow(
        key = Key(HtmlContent(messages("what-is-the-reason-for-the-report.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(messages("what-is-the-reason-for-the-report.what-is-the-reason-for-the-report." +
          in.reasonReport.getClass.getSimpleName.replace("$","")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("what-is-the-reason-for-the-report").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )

    val removalReason = in.removalReason.map { rr =>
      SummaryListRow(
        key = Key(HtmlContent(messages("why-should-it-be-removed.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(messages("why-should-it-be-removed.why-should-it-be-removed." + rr.getClass.getSimpleName.replace("$","")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("why-should-it-be-removed").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val otherReason = in.otherReason.map { or =>
      SummaryListRow(
        key = Key(HtmlContent(messages("other-reason.pageLabel")), "govuk-!-width-one-half"),
        value = Value(Text(or.value)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("other-reason").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    reasonReport :: List(removalReason,otherReason).flatten

  }

  def summaryList(in: Cr01Cr03Submission, messages: UniformMessages[Html]): SummaryList = {

    val baReport = SummaryListRow(
      key = Key(HtmlContent(messages("ba-report.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baReport)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("ba-report").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )
    val baRef = SummaryListRow(
      key = Key(HtmlContent(messages("ba-ref.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baRef)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("ba-ref").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val uprn = SummaryListRow(
      key = Key(HtmlContent(messages("UPRN.pageLabel")), "govuk-!-width-one-half"),
      value = Value(in.uprn.map(Text).getOrElse(HtmlContent(messages("summary.uprn.notEntered")))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("UPRN").url,
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
      key = Key(HtmlContent(messages("property-address.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(addressContent)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("property-address").url,
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
      key = Key(HtmlContent(messages("property-contact-details.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(contactDetailsContent)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("property-contact-details").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )
    val sameAddressQuestion = SummaryListRow(
      key = Key(HtmlContent(messages("same-contact-address.pageLabel")), "govuk-!-width-one-half"),
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
        contactAddress.line3.map(line3 => HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />")))
        ).getOrElse(Html("")),
        contactAddress.line4.map(line3 => HtmlFormat.fill(
          List(HtmlFormat.escape(line3), Html("<br />")))
        ).getOrElse(Html("")),
        HtmlFormat.escape(contactAddress.postcode)
      ))

      SummaryListRow(
        key = Key(HtmlContent(messages("contact-address.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(addressContent)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("contact-address").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    val effectiveDate = SummaryListRow(
      key = Key(HtmlContent(messages("effective-date.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(formatter.format(in.effectiveDate))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("effective-date").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val havePlanningRef = SummaryListRow(
      key = Key(HtmlContent(messages("have-planning-ref.pageLabel")), "govuk-!-width-one-half"),
      value = Value(HtmlContent(if(in.havePlaningReference) messages("have-planning-ref.have-planning-ref.Yes") else messages("have-planning-ref.have-planning-ref.No"))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.myJourney("have-planning-ref").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val planningRef = in.planningRef.map { planningRef =>
      SummaryListRow(
        key = Key(HtmlContent(messages("planning-ref.pageLabel")), "govuk-!-width-one-half"),
        value = Value(Text(planningRef)),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("planning-ref").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val noPlanningRef = in.noPlanningReference.map { noPlanningRef =>
      SummaryListRow(
        key = Key(HtmlContent(messages("why-no-planning-ref.pageLabel")), "govuk-!-width-one-half"),
        value = Value(HtmlContent(messages("why-no-planning-ref.why-no-planning-ref." + noPlanningRef.getClass.getSimpleName.replace("$","")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("why-no-planning-ref").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )
    }

    val comments =
      SummaryListRow(
        key = Key(HtmlContent(messages("comments.pageLabel")), "govuk-!-width-one-half"),
        value = Value(in.comments.map(Text).getOrElse(HtmlContent(messages("summary.comments.none")))),
        actions = Some(Actions(items = Seq(
          ActionItem(controllers.routes.UniformController.myJourney("comments").url,
            HtmlContent(messages("check-answers.changeLabel"))))
        ))
      )


    SummaryList(
      reasonSummaryList(in, messages) ++
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

  override def render(in: Cr01Cr03Submission, key: String, messages: UniformMessages[Html]): Html = {
    govUkSumaryList(summaryList(in, messages))
  }
}
