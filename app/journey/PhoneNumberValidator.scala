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

import cats.data.Validated
import ltbs.uniform.ErrorTree
import ltbs.uniform.validation.Rule

class PhoneNumberValidator extends Rule[String]{
  import PhoneNumberValidator._


  val validationRegex = """[0-9 \-]{1,20}"""

  override def apply(v1: String): Validated[ErrorTree, String] = {
    val updatePhoneNumber = allowedChars.replaceAllIn(v1, "")
    Rule.matchesRegex(validationRegex, "phoneNumber.format").apply(updatePhoneNumber)
  }
}

object PhoneNumberValidator {
  val allowedChars = """[^0-9 \-]""".r

  def apply(): PhoneNumberValidator = new PhoneNumberValidator

}
