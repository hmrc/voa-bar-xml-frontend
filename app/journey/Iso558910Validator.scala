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

import java.nio.charset.Charset

import cats.data.Validated
import ltbs.uniform.{ErrorMsg, ErrorTree}
import ltbs.uniform.validation.Rule

object Iso558910Validator {
  val isoCharser = Charset.forName("ISO-8859-15")
}

class Iso558910Validator extends Rule[String] {

  override def apply(v1: String): Validated[ErrorTree, String] = {
    val encoder = Iso558910Validator.isoCharser.newEncoder()
    if(v1.toCharArray.find(x => !encoder.canEncode(x)).isEmpty) {
      Validated.valid(v1)
    } else {
      Validated.invalid(ErrorTree.oneErr(ErrorMsg("error.invalidIsoString")))
    }
  }
}


