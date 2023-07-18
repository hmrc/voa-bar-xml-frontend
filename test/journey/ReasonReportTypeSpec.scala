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

import journey.ReasonReportType._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.libs.json.{JsString, Json}

import scala.reflect.runtime.{universe => ru}

class ReasonReportTypeSpec extends AnyFlatSpec with should.Matchers with TableDrivenPropertyChecks {

  "ReasonReportType" should "have all instance in Lister for to render radio buttons in order" in {
    val traitType = ru.typeOf[ReasonReportType]
    val traitClazz = traitType.typeSymbol.asClass

    traitClazz.knownDirectSubclasses.map(_.name.toString) should contain theSameElementsAs ReasonReportType.order
  }

  it should "deserialize" in {
    List(
      "AddProperty",
      "RemoveProperty"
    ).foreach{ reasonTypeString =>
      val json = JsString(reasonTypeString)
      Json.fromJson(json).isSuccess shouldBe true
    }
  }

  it should "serialize" in {
    List(
      AddProperty,
      RemoveProperty
    ).foreach { reasonReportType =>
      Json.toJson(reasonReportType) shouldBe JsString(reasonReportType.toString)
    }
  }

}
