/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import play.api.libs.json._
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}

case class Login (username: String, password: String)

object Login {
  implicit val format = Json.format[Login]

  def apply(username: String, password: String, encryptionRequired: Boolean):Login = {
    encryptionRequired match {
      case true => lazy val crypto = ApplicationCrypto.JsonCrypto
        Login (username, crypto.encrypt (PlainText (password) ).value)
      case false => Login(username, password)
    }
  }
}
