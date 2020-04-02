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

import cats.data.Validated.{Invalid, Valid}
import ltbs.uniform.ErrorTree
import org.scalatest.{EitherValues, FlatSpec, MustMatchers}
import org.scalatestplus.play.PlaySpec

class UniformJourneySpec extends FlatSpec with MustMatchers with EitherValues{

  import UniformJourney._

  "UniformJourney" should "validate BAReport" in {
    baReportValidation("1234") mustBe Valid("1234")
    baReportValidation("1") mustBe Valid("1")
    baReportValidation("123456789012") mustBe Valid("123456789012")
    baReportValidation("") mustBe a [Invalid[_]]
    baReportValidation("asdasd") mustBe a [Invalid[_]]
  }

  it should "validate BA-ref" in {
    baReferenceValidation("1234") mustBe Valid("1234")
    baReferenceValidation("adasd#$^&*()") mustBe Valid("adasd#$^&*()")
    baReferenceValidation("adasd#$^&*(%)") mustBe a [Invalid[_]]
  }

  it should "validate Address" in {
    val address = Address("99  Fosse Way", "ARDNAGOINE", None, Some("Fiction house"), "IV26 4YY")
    UniformJourney.addressValidation(address).toEither.right.value mustBe(address)
  }

  it should "reject invalid address" in {
    val address = Address("", "ARDNAGOINE", None, None, "IV26 4YY")
    UniformJourney.addressValidation(address).toEither.left.value mustBe a[ErrorTree]

  }



}
