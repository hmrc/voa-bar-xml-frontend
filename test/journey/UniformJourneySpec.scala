/*
 * Copyright 2021 HM Revenue & Customs
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

class UniformJourneySpec extends FlatSpec with MustMatchers with EitherValues{

  import UniformJourney._

  val string226Char = ((1 to 22).map(_ => "1234567890").mkString("")) + "123456"
  val string227Char = string226Char + "7"

  "UniformJourney" should "validate BAReport" in {
    baReportValidation("1234") mustBe Valid("1234")
    baReportValidation("1") mustBe Valid("1")
    baReportValidation("123456789012") mustBe Valid("123456789012")
    baReportValidation("") mustBe a [Invalid[_]]
    baReportValidation("asdasd") mustBe a [Valid[_]]
    baReportValidation("|") mustBe a [Invalid[_]]
  }

  it should "validate BA-ref" in {
    baReferenceValidation("1234") mustBe Valid("1234")
    baReferenceValidation("adasd#$^&*()") mustBe Valid("adasd#$^&*()")
    baReferenceValidation("adasd#$^&*(%)") mustBe a [Valid[_]]
    baReferenceValidation("|") mustBe a [Invalid[_]]
  }

  it should "validate UPRN" in {
    uprnValidation(None) mustBe Valid(None)
    uprnValidation(Some("1123")) mustBe Valid(Some("1123"))
    uprnValidation(Some("123456789011")) mustBe Valid(Some("123456789011"))
    uprnValidation(Some("1234567890013")) mustBe a [Invalid[_]]
    uprnValidation(Some("")) mustBe a [Invalid[_]]
  }

  it should "validate Address" in {
    val address = Address("99  Fosse Way %+", "ARDNAGOINE", None, Some("Fiction house"), "IV26 4YY")
    UniformJourney.longAddressValidation("some-address")(address).toEither.right.value mustBe(address)
  }

  it should "reject invalid address" in {
    val address = Address("", "ARDNAGOINE", None, None, "HHGGD")
    UniformJourney.longAddressValidation("some-address")(address).toEither.left.value mustBe a[ErrorTree]
  }

  it should "validate short address with 35 characters" in {
    val address = Address("12345678901234567890123456789012345", "ARDNAGOINE", None, Some("Fiction house"), "IV26 4YY")
    UniformJourney.shortAddressValidation("some-address")(address).toEither.right.value mustBe(address)
  }

  it should "reject short address with more that 35 characters" in {
    val address = Address("123456789012345678901234567890123456", "ARDNAGOINE", None, Some("Fiction house"), "IV26 4YY")
    UniformJourney.shortAddressValidation("some-address")(address).toEither.left.value mustBe a[ErrorTree]
  }

  it should "Validate correct contact details" in {
    val contactDetails = ContactDetails("First name", "lastName", None, None)
    UniformJourney.propertyContactDetailValidator(contactDetails).toEither.right.value mustBe(contactDetails)
  }

  it should "reject invalid contact details" in {
    val contactDetails = ContactDetails("", "", Some("*&*&"), Some("*&(*&"))
    UniformJourney.propertyContactDetailValidator(contactDetails).toEither.left.value mustBe a[ErrorTree]
    UniformJourney.propertyContactDetailValidator(contactDetails).toEither.left.value must have size(4)
  }

  it should "validate planning reference" in {
    UniformJourney.planningRefValidator("1234asdf½").toEither.left.value mustBe a[ErrorTree]
    UniformJourney.planningRefValidator("").toEither.left.value mustBe a[ErrorTree]
    UniformJourney.planningRefValidator("12345678901234567890123456").toEither.left.value mustBe a[ErrorTree]
    UniformJourney.planningRefValidator("€ \tŠ \tš \tŽ \tž \tŒ \tœ \tŸ").toEither.right.value mustBe ("€ \tŠ \tš \tŽ \tž \tŒ \tœ \tŸ")
  }

  it should "validate comments" in {
    UniformJourney.commentsValidation(Option(string226Char)).toEither.right.value mustBe (Option(string226Char))
    UniformJourney.commentsValidation(Option(string227Char)).toEither.left.value mustBe a[ErrorTree]
    UniformJourney.commentsValidation(Option("€ \tŠ \tš \tŽ \tž \tŒ \tœ \tŸ")).toEither.right.value mustBe(Some("€ \tŠ \tš \tŽ \tž \tŒ \tœ \tŸ"))
  }



}
