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

package utils

object Formats {

  import play.api.libs.json._

  implicit val uniformDBFormat: Format[Map[List[String], String]] = new Format[Map[List[String], String]] {

    override def writes(o: Map[List[String], String]): JsValue = {
      val data = o.map { case (key, value) =>
        (key.mkString("."), JsString(value)) // Todo, maybe better delimiter
      }
      JsObject(data)
    }

    override def reads(json: JsValue): JsResult[Map[List[String], String]] = json match {
      case JsObject(data) =>
        val dataMap = data.map { case (key, value) =>
          value match {
            case JsString(stringValue) =>
              (key.split('.').toList, stringValue)
            case _                     =>
              throw new RuntimeException(s"Unable to deserialize $value")
          }
        }.toMap
        JsSuccess(dataMap)
      case x              => JsError(s"unable to parse : $x")
    }
  }

}
