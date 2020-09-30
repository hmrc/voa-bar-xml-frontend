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

sealed trait CouncilTaxBandType

case object A extends CouncilTaxBandType
case object B extends CouncilTaxBandType
case object C extends CouncilTaxBandType
case object D extends CouncilTaxBandType
case object E extends CouncilTaxBandType
case object F extends CouncilTaxBandType
case object G extends CouncilTaxBandType
case object H extends CouncilTaxBandType
case object I extends CouncilTaxBandType


object CouncilTaxBandType {
  val order = List[String] (
    "A",
    "B",
    "C",
    "D",
    "E",
    "F",
    "G",
    "H",
    "I"
  )

  implicit val format: Format[CouncilTaxBandType] = new Format[CouncilTaxBandType] {
    override def reads(json: JsValue): JsResult[CouncilTaxBandType] = {
      json match  {
        case JsString("A") => JsSuccess(A)
        case JsString("B") => JsSuccess(B)
        case JsString("C") => JsSuccess(C)
        case JsString("D") => JsSuccess(D)
        case JsString("E") => JsSuccess(E)
        case JsString("F") => JsSuccess(F)
        case JsString("G") => JsSuccess(G)
        case JsString("H") => JsSuccess(H)
        case JsString("I") => JsSuccess(I)
        case x => JsError(s"Unable to deserialize ReasonReportType $x")
      }
    }

    override def writes(o: CouncilTaxBandType): JsValue = {
      o match  {
        case A => JsString("A")
        case B => JsString("B")
        case C => JsString("C")
        case D => JsString("D")
        case E => JsString("E")
        case F => JsString("F")
        case G => JsString("G")
        case H => JsString("H")
        case I => JsString("I")
      }
    }
  }
}

