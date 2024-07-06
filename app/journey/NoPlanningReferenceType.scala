/*
 * Copyright 2024 HM Revenue & Customs
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

sealed trait NoPlanningReferenceType extends Product with Serializable

case object WithoutPlanningPermission extends NoPlanningReferenceType
case object NotApplicablePlanningPermission extends NoPlanningReferenceType
case object NotRequiredPlanningPermission extends NoPlanningReferenceType
case object PermittedDevelopment extends NoPlanningReferenceType
case object NoPlanningApplicationSubmitted extends NoPlanningReferenceType

object NoPlanningReferenceType {

  val order = List[String](
    "WithoutPlanningPermission",
    "NotApplicablePlanningPermission",
    "NotRequiredPlanningPermission",
    "PermittedDevelopment",
    "NoPlanningApplicationSubmitted"
  )

  implicit val format: Format[NoPlanningReferenceType] = new Format[NoPlanningReferenceType] {

    override def reads(json: JsValue): JsResult[NoPlanningReferenceType] =
      json match {
        case JsString("WithoutPlanningPermission")       => JsSuccess(WithoutPlanningPermission)
        case JsString("NotApplicablePlanningPermission") => JsSuccess(NotApplicablePlanningPermission)
        case JsString("NotRequiredPlanningPermission")   => JsSuccess(NotRequiredPlanningPermission)
        case JsString("PermittedDevelopment")            => JsSuccess(PermittedDevelopment)
        case JsString("NoPlanningApplicationSubmitted")  => JsSuccess(NoPlanningApplicationSubmitted)
        case x                                           => JsError(s"Unable to deserialize NoPlanningReferenceType $x")
      }

    override def writes(o: NoPlanningReferenceType): JsValue =
      o match {
        case WithoutPlanningPermission       => JsString("WithoutPlanningPermission")
        case NotApplicablePlanningPermission => JsString("NotApplicablePlanningPermission")
        case NotRequiredPlanningPermission   => JsString("NotRequiredPlanningPermission")
        case PermittedDevelopment            => JsString("PermittedDevelopment")
        case NoPlanningApplicationSubmitted  => JsString("NoPlanningApplicationSubmitted")
      }
  }

}
