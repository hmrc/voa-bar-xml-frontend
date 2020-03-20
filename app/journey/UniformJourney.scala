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
import ltbs.uniform._
import cats.implicits._
import ltbs.uniform.validation.Rule
import ltbs.uniform.validation.Rule.{maxLength, minLength}

import scala.language.higherKinds

object UniformJourney {

  case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String)
  case class ComplexForm(baReport: String, baRef: String, address: Address)

  type AskTypes =  String :: NilTypes
  type TellTypes = Long :: NilTypes


  def ctTaxJourney[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[ComplexForm] = {
    import interpreter._

    for {
      baReport <- ask[String]("ba-report", validation = baReportValidation )
      baRef <- ask[String]("ba-ref", validation = baReferenceValidation)
      address <- ask[String]("property-address")
    } yield ComplexForm(baReport, baRef, Address(address, "", None, None, ""))
  }

  def baReportValidation(a: String) = {
    lengthBetween(1, 12, "ba-report.error.minLenght", "ba-report.error.maxLenght").apply(a) andThen (
      Rule.matchesRegex("\\d+", "ba-report.error.number").apply(_))
  }

  def baReferenceValidation(a: String) = {
    // [A-Za-z0-9\s~!&quot;@#$%&amp;'\(\)\*\+,\-\./:;&lt;=&gt;\?\[\\\]_\{\}\^&#xa3;&#x20ac;]*
    lengthBetween(1, 25, "ba-ref.error.minLenght", "ba-ref.error.maxLenght").apply(a) andThen ((
      Rule.matchesRegex("""[A-Za-z0-9\s\~!"@#\$&;'\(\)\*,\-\./:;<=>\?\[\\\]_\{\}\^£€]*""", "ba-ref.error.allowedChars").apply(_)))
  }

  def lengthBetween(min: Int, max: Int, minMessage: String, maxMessage:String) = new Rule[String] {
    override def apply(v1: String): Validated[ErrorTree, String] = {
        minLength[String](min, minMessage).apply(v1) andThen (maxLength[String](max, maxMessage).apply(_))
    }
  }

}

