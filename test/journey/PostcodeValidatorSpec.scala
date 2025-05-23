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

import ltbs.uniform.ErrorTree
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import org.scalatest.EitherValues

class PostcodeValidatorSpec extends AnyFlatSpec with should.Matchers with EitherValues {

  val validator = new PostcodeValidator()

  "Postcode validator" should "validate valid postcode" in {
    validator("BN12 4AX").toEither.value shouldBe "BN12 4AX"
  }

  it should "strip not postcode characters and format postcode" in {
    validator("-- B N1   2 4A  X  ").toEither.value     shouldBe "BN12 4AX"
    validator("-(&^^%*$&%^#- BN12 4A x").toEither.value shouldBe "BN12 4AX"
    validator("bn124ax:").toEither.value                shouldBe "BN12 4AX"
    validator("e2-6b J:").toEither.value                shouldBe "E2 6BJ"
    validator("e2-6b,. J:").toEither.value              shouldBe "E2 6BJ"
    // bn12 4axx and bbn12 4ax
  }

  it should "reject invalid postcode" in {
    validator("1112 4AX").toEither.left.value shouldBe a[ErrorTree]
  }

  it should "reject empty postcode" in {
    validator("").toEither.left.value shouldBe a[ErrorTree]
  }

}
