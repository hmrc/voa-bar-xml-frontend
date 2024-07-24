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

package forms.validator

/**
  * Copy of hmrc-deskpro NameValidator.
  *
  * @see https://github.com/hmrc/hmrc-deskpro/blob/main/app/model/deskpro/NameValidator.scala
  */
case class NameValidator() {

  val nameRegex: String = """^[A-Za-z\-.,()'"\s]+$"""

  // Quick check that the string does not contain "http://" or "https://" before the full regex
  def validate(name: String): Boolean =
    !name.contains("http://") && !name.contains("https://") && name.matches(nameRegex)

}
