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

import journey.UniformJourney.Cr05Common
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.html.components.govukSummaryList
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

// TODO test
// $COVERAGE-OFF$
class Cr05CommonWebTell(govUkSumaryList: govukSummaryList) extends GenericWebTell[Cr05Common, Html] {

  def confirmationSummary(in: Cr05Common, messages: Messages): SummaryList = {
    val sum = summaryList(in, new UniformMessages[Html] {
      override def get(key: String, args: Any*): Option[Html] = Option(Html(messages(key, args)))

      override def list(key: String, args: Any*): List[Html] = Nil
    })
    SummaryList(sum.rows.map(x => x.copy(actions = None)), "govuk-!-margin-bottom-9")
  }

  def summaryList(in: Cr05Common, messages: UniformMessages[Html]): SummaryList = {

    val baReport = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-ba-report.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baReport)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addCommonSectionJourney("add-property-ba-report").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )
    val baRef = SummaryListRow(
      key = Key(HtmlContent(messages("add-property-ba-ref.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(in.baRef)),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addCommonSectionJourney("add-property-ba-ref").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    val effectiveDate = SummaryListRow(
      key = Key(HtmlContent(messages("effective-date.pageLabel")), "govuk-!-width-one-half"),
      value = Value(Text(formatter.format(in.effectiveDate))),
      actions = Some(Actions(items = Seq(
        ActionItem(controllers.routes.UniformController.addCommonSectionJourney("add-property-effective-date").url,
          HtmlContent(messages("check-answers.changeLabel"))))
      ))
    )

    SummaryList(
        Seq(
          Option(baReport),
          Option(baRef),
          Option(effectiveDate)

        ).flatten)
  }

  override def render(in: Cr05Common, key: String, messages: UniformMessages[Html]): Html =
    govUkSumaryList(summaryList(in, messages))
}
// $COVERAGE-ON$
