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

package journey

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

sealed trait RemovalReasonType

case object Demolition extends RemovalReasonType
case object Disrepair extends RemovalReasonType
case object Renovating extends RemovalReasonType
case object NotComplete extends RemovalReasonType
case object BandedTooSoon extends RemovalReasonType
case object CaravanRemoved extends RemovalReasonType
case object Duplicate extends RemovalReasonType
case object OtherReason extends RemovalReasonType


object RemovalReasonType {
  val order = List[String] (
    "Demolition",
    "Disrepair",
    "Renovating",
    "NotComplete",
    "BandedTooSoon",
    "CaravanRemoved",
    "Duplicate",
    "OtherReason"
  )

  implicit val format: Format[RemovalReasonType] = new Format[RemovalReasonType] {
    override def reads(json: JsValue): JsResult[RemovalReasonType] = {
      json match  {
        case JsString("It has been demolished") => JsSuccess(Demolition)
        case JsString("It is in disrepair") => JsSuccess(Disrepair)
        case JsString("It is being renovated") => JsSuccess(Renovating)
        case JsString("It is not complete") => JsSuccess(NotComplete)
        case JsString("It has been banded too soon") => JsSuccess(BandedTooSoon)
        case JsString("The caravan has been removed from the site") => JsSuccess(CaravanRemoved)
        case JsString("It is a duplicate of another property") => JsSuccess(Duplicate)
        case JsString("Other reason") => JsSuccess(OtherReason)
        case x => JsError(s"Unable to deserialize RemovalReasonType $x")
      }
    }

    override def writes(o: RemovalReasonType): JsValue = {
      o match  {
        case Demolition       => JsString("It has been demolished")
        case Disrepair        => JsString("RemoveProperty")
        case Renovating       => JsString("AddProperty")
        case NotComplete      => JsString("RemoveProperty")
        case BandedTooSoon    => JsString("AddProperty")
        case CaravanRemoved   => JsString("RemoveProperty")
        case Duplicate        => JsString("AddProperty")
        case OtherReason      => JsString("RemoveProperty")
      }
    }
  }
}

