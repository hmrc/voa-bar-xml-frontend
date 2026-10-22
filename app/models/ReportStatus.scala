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

package models

import models.ReportStatus.dateShortFilenameLabelFormat

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import play.api.i18n.Messages
import play.api.libs.json.*
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats.mongoEntity
import uk.gov.hmrc.vo.service.conversion.DateConversion.*

sealed trait ReportStatusType:

  val value: String =
    val a: Class[? <: ReportStatusType] = getClass.asSubclass(getClass)
    a.getSimpleName.replace("$", "")

case object Pending extends ReportStatusType
case object Verified extends ReportStatusType
case object Failed extends ReportStatusType
case object Unknown extends ReportStatusType
case object Submitted extends ReportStatusType
case object Cancelled extends ReportStatusType
case object Done extends ReportStatusType

case class ReportErrorDetail(errorCode: String, values: Seq[String] = Seq.empty[String])

object ReportErrorDetail:
  implicit val format: OFormat[ReportErrorDetail] = Json.format

case class ReportError(reportNumber: Option[String], baTransaction: Option[String], uprn: Seq[Long], errors: Seq[ReportErrorDetail])

object ReportError:
  implicit val format: OFormat[ReportError] = Json.format

object ReportStatus:

  import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.*

  implicit val format: Format[ReportStatus] = mongoEntity {
    Json.format
  }

  val dateShortFilenameLabelFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

final case class ReportStatus(
  id: String,
  url: Option[String] = None,
  checksum: Option[String] = None,
  errors: Seq[Error] = Seq(),
  reportErrors: Seq[ReportError] = Seq(),
  baCode: Option[String] = None,
  status: Option[String] = Some(Pending.value),
  filename: Option[String] = None,
  totalReports: Option[Int] = None,
  report: Option[JsObject] = None,
  createdAt: Instant = Instant.now
):

  def createdAtShortFilenameLabel: String = createdAt.atZone(ZoneOffset.UTC).format(dateShortFilenameLabelFormat)

  def createdAtInCSV: String = createdAt.formatIsoDateTimeUTC

  def createdAtZonedUTC: ZonedDateTime = createdAt.atZone(ZoneOffset.UTC)

  def title(messages: Messages): String =
    val defaultStatus = status.getOrElse(Pending.value).toLowerCase
    messages(s"confirmation.heading.$defaultStatus")
