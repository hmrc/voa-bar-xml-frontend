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

import ltbs.uniform.ErrorTree
import org.scalatest.{EitherValues, FlatSpec, Matchers}

class PhoneNumberVarlidatorSpec extends FlatSpec with Matchers with EitherValues {

  val validator = new PhoneNumberValidator()

  "validator" should "validate correct phone number" in {
    validator.apply("01632960110").toEither.right.value shouldBe "01632960110"
    validator.apply("02233323233333221123").toEither.right.value shouldBe "02233323233333221123"
  }

  it should "Validate and stript non allowed characters from phone number" in {
    validator.apply("0##@$$1632 960110").toEither.right.value shouldBe "01632 960110"
    validator.apply("0##@$$1632-960110").toEither.right.value shouldBe "01632-960110"
    validator.apply("   0##@$$1632 960110").toEither.right.value shouldBe "   01632 960110"
  }

  it should "Reject invalid phone number" in {
    validator.apply("asdasdad").toEither.left.value shouldBe a[ErrorTree]
    validator.apply("022333232333332211232").toEither.left.value shouldBe a[ErrorTree]
    validator.apply("").toEither.left.value shouldBe a[ErrorTree]
  }


}
