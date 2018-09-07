/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.OffsetDateTime

import play.api.libs.json._
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

sealed trait ReportStatusType { val value: String = getClass.asSubclass(getClass).getSimpleName.replace("$", "") }
case object Pending extends ReportStatusType
case object UpScanVerified extends ReportStatusType
case object UpscanFailed extends ReportStatusType
case object XmlValidationFailed extends ReportStatusType

object ReportStatus {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val errorFormat = Json.format[ReportStatusError]
  implicit val format = Json.format[ReportStatus]
  val name = classOf[ReportStatus].getSimpleName.toLowerCase
  val key = "_id"
}
final case class ReportStatusError(
                                    errorCode: String,
                                    message: String,
                                    detail: String
                                  )
final case class ReportStatus(
                               _id: String,
                               date: OffsetDateTime,
                               url: Option[String] = None,
                               checksum: Option[String] = None,
                               errors: Seq[ReportStatusError] = Seq(),
                               userId: Option[String] = None,
                               status: Option[String] = Some(Pending.value)
                             )