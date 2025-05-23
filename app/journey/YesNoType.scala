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

import play.api.libs.json.{Format, JsResult, JsString, JsSuccess, JsValue}

sealed trait YesNoType

case object Yes extends YesNoType
case object No extends YesNoType

object YesNoType {

  implicit val format: Format[YesNoType] = new Format[YesNoType] {

    override def reads(json: JsValue): JsResult[YesNoType] =
      json match {
        case JsString("Yes") => JsSuccess(Yes)
        case _               => JsSuccess(No)
      }

    override def writes(o: YesNoType): JsValue =
      JsString(if o == Yes then "Yes" else "No")
  }

}
