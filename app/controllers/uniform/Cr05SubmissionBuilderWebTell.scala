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

import journey.UniformJourney.Cr05SubmissionBuilder
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Value}
import views.html.govuk.cr05SubmissionSummary

// TODO test
// $COVERAGE-OFF$
class Cr05SubmissionBuilderWebTell(cr05SubmissionSummary: cr05SubmissionSummary) extends GenericWebTell[Cr05SubmissionBuilder, Html] {

  def confirmationSummary(in: Cr05SubmissionBuilder, messagess: UniformMessages[Html]): SummaryList = {
    val sum = commentsSummaryList(in, messagess)
    SummaryList(sum.rows.map(x => x.copy(actions = None)), classes = "govuk-!-margin-bottom-9")
  }

  def commentsSummaryList(in: Cr05SubmissionBuilder, messages: UniformMessages[Html]): SummaryList =
    SummaryList(
      List(
        SummaryListRow(
          key = Key(HtmlContent(messages("comments.pageLabel")), "govuk-!-width-one-half"),
          value = Value(in.comments.map(Text.apply).getOrElse(HtmlContent(messages("summary.comments.none")))),
          actions = Some(Actions(items =
            Seq(
              ActionItem(controllers.routes.UniformController.addCommentJourney().url, HtmlContent(messages("check-answers.changeLabel")))
            )
          ))
        )
      )
    )

  override def render(in: Cr05SubmissionBuilder, key: String, messages: UniformMessages[Html]): Html =
    cr05SubmissionSummary(in, messages)
}
// $COVERAGE-ON$
