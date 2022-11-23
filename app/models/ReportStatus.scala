/*
 * Copyright 2022 HM Revenue & Customs
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

import models.ReportStatus.{createdDateUIFormat, createdDateUILongFormat, dateShortFormat}

import java.time.{Instant, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats.mongoEntity

sealed trait ReportStatusType {
  val value: String = {
    val a: Class[_ <: ReportStatusType] = getClass.asSubclass(getClass)
    val u: String = a.getSimpleName.replace("$", "")
    u
  }
}

case object Pending extends ReportStatusType
case object Verified extends ReportStatusType
case object Failed extends ReportStatusType
case object Unknown extends ReportStatusType
case object Submitted extends ReportStatusType
case object Cancelled extends ReportStatusType
case object Done extends ReportStatusType


case class ReportErrorDetail(errorCode: String, values: Seq[String] = Seq.empty[String])

object ReportErrorDetail {
  implicit val format = Json.format[ReportErrorDetail]

}

case class ReportError(reportNumber: Option[String],
                       baTransaction: Option[String],
                       uprn: Seq[Long],
                       errors: Seq[ReportErrorDetail]
                      )

object ReportError {
  implicit val format = Json.format[ReportError]
}


object ReportStatus {

  import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits._

  implicit val format: Format[ReportStatus] = mongoEntity {
    Json.format[ReportStatus]
  }

  val createdDateUIFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

  val createdDateUILongFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' kk:mm")

  val dateShortFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

}

final case class ReportStatus(
                               id: String,
                               // TODO: After 1 April 2023 remove property 'created' as only 'createdAt' is used
                               created: Option[ZonedDateTime] = None,
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
                             ){

  def formattedCreated: String = createdAtFormatted(createdDateUIFormat)

  def formattedCreatedLong: String = createdAtFormatted(createdDateUILongFormat)

  def formattedCreatedShort: String = createdAtFormatted(dateShortFormat)

  def createdInCSV: String = createdAtFormatted(DateTimeFormatter.ISO_DATE_TIME)

  def createdAtZoned: ZonedDateTime = createdAt.atZone(ZoneOffset.UTC)

  private def createdAtFormatted(formatter: DateTimeFormatter): String =
    createdAt.atZone(ZoneOffset.UTC).format(formatter)

  def title(messages: Messages): String = {
    val defaultStatus = status.getOrElse(Pending.value).toLowerCase
    messages(s"confirmation.heading.$defaultStatus")
  }

}
