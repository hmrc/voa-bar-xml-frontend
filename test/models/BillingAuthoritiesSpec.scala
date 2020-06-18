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

package models

import base.SpecBase

class BillingAuthoritiesSpec extends SpecBase {

  val existingBaCode = "BA0230"
  val nonExistingBaCode = "ba9999"
  val ForestHeath = "ba3510"
  val StEdmundsbury = "ba3525"
  val SuffolkCoastal = "ba3530"
  val Waveney = "ba3535"
  val BAMapSize = 350

  "BillingAuthorities" must {

    "Return the name of the Billing Authority for an existing Billing Authority Code" in {
      BillingAuthorities.find(existingBaCode) mustBe Some("Luton")
    }

    "Return None if no baCode is found related to the given code even if the user is logged in" in {
      BillingAuthorities.find(nonExistingBaCode) mustBe None
    }

    "have 350 entries" in {
      BillingAuthorities.billingAuthorities.size mustBe BAMapSize
    }

    "Return None if Forest Heath Code is found related to the given code even if the user is logged in" in {
      BillingAuthorities.find(ForestHeath) mustBe None
    }

    "Return None if St Edmundsbury Code is found related to the given code even if the user is logged in" in {
      BillingAuthorities.find(StEdmundsbury) mustBe None
    }

    "Return None if Suffolk Coastal Code is found related to the given code even if the user is logged in" in {
      BillingAuthorities.find(SuffolkCoastal) mustBe None
    }

    "Return None if Waveney Code is found related to the given code even if the user is logged in" in {
      BillingAuthorities.find(Waveney) mustBe None
    }
  }
}
