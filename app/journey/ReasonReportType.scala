/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.data.FormError
import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import play.api.data.format.Formatter

sealed trait ReasonReportType extends Product with Serializable

case object AddProperty extends ReasonReportType
case object SplitProperty extends ReasonReportType
//case object MergeProperty extends ReasonReportType
case object RemoveProperty extends ReasonReportType


object ReasonReportType {
  val order = List[String](
    "AddProperty",
    "RemoveProperty",
    "SplitProperty"
  )

  implicit val format: Format[ReasonReportType] = new Format[ReasonReportType] {
    override def reads(json: JsValue): JsResult[ReasonReportType] = {
      json match {
        case JsString("AddProperty") => JsSuccess(AddProperty)
        case JsString("SplitProperty") => JsSuccess(SplitProperty)
        //        case JsString("MergeProperty") => JsSuccess(MergeProperty)
        case JsString("RemoveProperty") => JsSuccess(RemoveProperty)
        case x => JsError(s"Unable to deserialize ReasonReportType $x")
      }
    }

    override def writes(o: ReasonReportType): JsValue = {
      o match {
        case AddProperty => JsString("AddProperty")
        case SplitProperty       => JsString("SplitProperty")
        //        case MergeProperty       => JsString("MergeProperty")
        case RemoveProperty => JsString("RemoveProperty")
      }
    }
  }

  implicit val formFormat: Formatter[ReasonReportType] = new Formatter[ReasonReportType] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], ReasonReportType] = {
      data.get(key).map {
        case "AddProperty" => AddProperty
        case "RemoveProperty" => RemoveProperty
        case "SplitProperty" => SplitProperty
        case x => throw new RuntimeException(s"Unknown reson type: ${x} ")
      }.map(x => Right(x))
       .getOrElse(Left(Seq(FormError(key, "what-is-the-reason-for-the-report.required"))))
    }

    override def unbind(key: String, value: ReasonReportType): Map[String, String] = value match {
      case AddProperty => Map(key -> "AddProperty")
      case SplitProperty => Map(key -> "SplitProperty")
      //case MergeProperty => Map(key -> "MergeProperty")
      case RemoveProperty => Map(key -> "RemoveProperty")
    }
  }

}

