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

import base.SpecBase
import play.api.Configuration
import uk.gov.hmrc.crypto.{ApplicationCryptoDI, Crypted}

class LoginSpec extends SpecBase {

  val configuration = injector.instanceOf[Configuration]
  val username = "user"
  val password = "pass"

  "Given an username and password produce a login model containing plain text password" in {
    val result = Login(username, password)

    result.username mustBe username
    result.password mustBe password
  }

  "Given an username and password, the encrypt method should return an encryted Login model" in {
     lazy val crypto = new ApplicationCryptoDI(configuration).JsonCrypto
     val result = Login(username, password).encrypt

    result.username mustBe username
    password mustBe crypto.decrypt(Crypted(result.password)).value
  }
}
