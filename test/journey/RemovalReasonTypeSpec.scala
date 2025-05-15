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

import journey.RemovalReasonType.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsString, Json}
import utils.ReflectionUtil

class RemovalReasonTypeSpec extends AnyFlatSpec with should.Matchers with TableDrivenPropertyChecks {

  "RemovalReasonType" should "have all instance in Lister for to render radio buttons in order" in {

    ReflectionUtil.findAllSubTypeNames[RemovalReasonType] should contain theSameElementsAs RemovalReasonType.order
  }

  it should "deserialize" in
    List(
      "Demolition",
      "Disrepair",
      "Derelict",
      "Renovating",
      "BandedTooSoonOrNotComplete",
      "CaravanRemoved",
      "Duplicate",
      "OtherReason"
    ).foreach { removalReasonTypeString =>
      val json     = JsString(removalReasonTypeString)
      val jsResult = Json.fromJson(json)
      jsResult.isSuccess shouldBe true
      val noPlanningReferenceType = jsResult.get
      noPlanningReferenceType.toString shouldBe removalReasonTypeString
    }

  it should "serialize" in
    List(
      Demolition,
      Disrepair,
      Derelict,
      Renovating,
      BandedTooSoonOrNotComplete,
      CaravanRemoved,
      Duplicate,
      OtherReason
    ).foreach { removalReasonType =>
      Json.toJson(removalReasonType)(using RemovalReasonType.format) shouldBe JsString(removalReasonType.toString)
    }

}
