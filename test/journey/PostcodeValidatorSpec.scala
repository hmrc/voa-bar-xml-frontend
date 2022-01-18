/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class PostcodeValidatorSpec extends FlatSpec with Matchers with EitherValues {

  val validator = new PostcodeValidator()

  "Postcode validator" should "validate valid postcode" in {
    validator("BN12 4AX").toEither.right.value shouldBe ("BN12 4AX")
  }

  it should "strip not postcode characters and format postcode" in {
    validator("-- B N1   2 4A  X  ").toEither.right.value shouldBe ("BN12 4AX")
    validator("-(&^^%*$&%^#- BN12 4A x").toEither.right.value shouldBe ("BN12 4AX")
    validator("bn124ax:").toEither.right.value shouldBe ("BN12 4AX")
    validator("e2-6b J:").toEither.right.value shouldBe ("E2 6BJ")
    validator("e2-6b,. J:").toEither.right.value shouldBe ("E2 6BJ")
    //bn12 4axx and bbn12 4ax
  }



  it should "reject invalid postcode" in {
    validator("1112 4AX").toEither.left.value shouldBe a[ErrorTree]
  }

  it should "reject empty postcode" in {
    validator("").toEither.left.value shouldBe a[ErrorTree]
  }



}
