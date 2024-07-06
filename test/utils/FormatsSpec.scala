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

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class FormatsSpec extends AnyFlatSpec with must.Matchers {

  val uniformData: Map[List[String], String] = Map(
    List("simple", "path") -> "value in simple path",
    List("OneElementPath") -> "value in oneElementPath"
  )

  val jsonData = Json.obj("simple.path" -> "value in simple path", "OneElementPath" -> "value in oneElementPath")

  "uniformDBFormat" should "write uniform DB data to Json" in {
    Formats.uniformDBFormat.writes(uniformData) mustBe jsonData
  }

  it should "read data from json to UniformDB format" in {
    Formats.uniformDBFormat.reads(jsonData) mustBe JsSuccess(uniformData)
  }

  it should "not read data from json for unsuported format" in {
    val data = JsString("value")
    Formats.uniformDBFormat.reads(data) mustBe a[JsError]
  }

}
