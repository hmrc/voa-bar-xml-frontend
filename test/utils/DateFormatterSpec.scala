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

package utils

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must

import java.time.{ZoneId, ZonedDateTime}

class DateFormatterSpec extends AnyFlatSpec with must.Matchers {

  "DateFormatter" should "format date" in {
    val dateTime = ZonedDateTime.of(
      2020,
      10,
      1,
      10,
      0,
      0,
      0,
      ZoneId.of("Europe/London")
    )
    DateFormatter.formatDate(dateTime) mustBe "01 October 2020 at 10:00"
  }

}
