/*
 * Copyright 2026 HM Revenue & Customs
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

import ltbs.uniform.{ErrorMsg, ErrorTree}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.EitherValues
import org.scalatest.prop.TableDrivenPropertyChecks

class Iso558910ValidatorSpec extends AnyFlatSpec with should.Matchers with EitherValues with TableDrivenPropertyChecks {

  val invalidInputData = Table(
    "Incorrect string",
    "½",
    "🤔",
    "Ω≈ç√∫˜µ≤"
  )

  val validInputData = Table(
    "Correct string",
    "€ \tŠ \tš \tŽ \tž \tŒ \tœ \tŸ",
    "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s"
  )

  "Iso558910ValidatorSpec" should "Reject all non iso8859-15 characters" in {
    forAll(invalidInputData) { badString =>
      Iso558910Validator(badString).toEither.left.value shouldBe ErrorTree.oneErr(ErrorMsg("error.invalidIsoString"))
    }
  }

  it should "Allow all valid charactes in iso8859-15" in {
    forAll(validInputData) { goodString =>
      Iso558910Validator(goodString).toEither.value shouldBe goodString
    }

  }

}
