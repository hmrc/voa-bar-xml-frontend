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

import cats.data.Validated
import ltbs.uniform.ErrorTree
import ltbs.uniform.validation.Rule
//import collection.JavaConverters._


class PostcodeValidator extends Rule[String]{

  val allowedChars = """[^a-zA-Z0-9]""".r

  override def apply(v1: String): Validated[ErrorTree, String] = {
    Rule.minLength[String](1, "property-address.postcode.minLength").apply(v1) andThen { validLenght =>
      val cleanedPostcode = allowedChars.replaceAllIn(validLenght, "").toUpperCase
      Rule.matchesRegex("""^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$""",
        "property-address.postcode.eror").apply(cleanedPostcode).map { validPostcode =>
        validPostcode.substring(0, validPostcode.length - 3) + " " + validPostcode.substring(validPostcode.length - 3)
      }
    }
  }
}
