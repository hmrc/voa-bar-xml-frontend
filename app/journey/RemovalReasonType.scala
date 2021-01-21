/*
 * Copyright 2021 HM Revenue & Customs
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

package journey

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait RemovalReasonType

case object Demolition extends RemovalReasonType
case object Disrepair extends RemovalReasonType
case object Derelict extends RemovalReasonType
case object Renovating extends RemovalReasonType
case object BandedTooSoonOrNotComplete extends RemovalReasonType
case object CaravanRemoved extends RemovalReasonType
case object Duplicate extends RemovalReasonType
case object OtherReason extends RemovalReasonType


object RemovalReasonType {
  val order = List[String] (
    "Demolition",
    "Disrepair",
    "Derelict",
    "Renovating",
    "BandedTooSoonOrNotComplete",
    "CaravanRemoved",
    "Duplicate",
    "OtherReason"
  )

  implicit val format: Format[RemovalReasonType] = new Format[RemovalReasonType] {
    override def reads(json: JsValue): JsResult[RemovalReasonType] = {
      json match  {
        case JsString("Demolition") => JsSuccess(Demolition)
        case JsString("Disrepair") => JsSuccess(Disrepair)
        case JsString("Derelict") => JsSuccess(Derelict)
        case JsString("Renovating") => JsSuccess(Renovating)
        case JsString("BandedTooSoonOrNotComplete") => JsSuccess(BandedTooSoonOrNotComplete)
        case JsString("CaravanRemoved") => JsSuccess(CaravanRemoved)
        case JsString("Duplicate") => JsSuccess(Duplicate)
        case JsString("OtherReason") => JsSuccess(OtherReason)
        case x => JsError(s"Unable to deserialize RemovalReasonType $x")
      }
    }

    override def writes(o: RemovalReasonType): JsValue = {
      o match  {
        case Demolition                 => JsString("Demolition")
        case Disrepair                  => JsString("Disrepair")
        case Derelict                   => JsString("Derelict")
        case Renovating                 => JsString("Renovating")
        case BandedTooSoonOrNotComplete => JsString("BandedTooSoonOrNotComplete")
        case CaravanRemoved             => JsString("CaravanRemoved")
        case Duplicate                  => JsString("Duplicate")
        case OtherReason                => JsString("OtherReason")
      }
    }
  }
}

