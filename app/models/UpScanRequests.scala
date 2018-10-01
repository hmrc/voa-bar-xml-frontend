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

import play.api.libs.json.Json

object UpScanRequests {
  implicit val initiateRequest = Json.format[InitiateRequest]
  implicit val uploadRequests = Json.format[UploadRequest]
  implicit val initialResponse = Json.format[InitiateResponse]
  implicit val uploadDetails = Json.format[UploadDetails]
  implicit val uploadConfirmation = Json.format[UploadConfirmation]
  case class InitiateRequest(
                              callbackUrl: String,
                              maxFileSize: Int
                            )

  case class InitiateResponse(
                               reference: String,
                               uploadRequest: UploadRequest
                             )

  case class UploadRequest(
                            href: String,
                            fields: Map[String, String]
                          )

  case class UploadConfirmation (
                                reference: String,
                                downloadUrl: String,
                                fileStatus: String,
                                uploadDetails: UploadDetails
                                )

  case class UploadDetails (
                           uploadTimestamp: OffsetDateTime,
                           checksum: String,
                           fileMimeType: String,
                           fileName: String
                           )
}