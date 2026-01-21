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

package views

import controllers.ReverseConfirmationController

import javax.inject.Inject
import models.ReportStatus
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.TableRow
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class TableFormatter @Inject() (serviceConfig: ServicesConfig) {

  def formatSummaryLink(reportStatus: ReportStatus, confirmationController: ReverseConfirmationController)(implicit messages: Messages) =
    s"<a href='${confirmationController.onPageRefresh(reportStatus.id)}'>${formatStatuslink(reportStatus.status)}</a>"

  def formatStatuslink(value: Option[String])(implicit messages: Messages): String = value match {
    case Some("Done")      => Messages("confirmation.heading.submitted")
    case Some("Submitted") => Messages("confirmation.heading.submitted")
    case _                 => Messages("confirmation.heading.failed")
  }

  def formatRows(reportStatuses: Seq[ReportStatus], confirmationController: ReverseConfirmationController)(implicit messages: Messages) =
    reportStatuses.map(reportStatus =>
      Seq(
        TableRow(Text(reportStatus.formattedCreated)),
        TableRow(Text(reportStatus.id.split("-").head)),
        TableRow(Text(reportStatus.filename.getOrElse("Unknown"))),
        TableRow(HtmlContent(formatSummaryLink(reportStatus, confirmationController)))
      )
    )
}
