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

import cats.data.{NonEmptyList, Validated}
import ltbs.uniform._
import cats.implicits._
import ltbs.uniform.validation.Rule
import ltbs.uniform.validation.Rule.{error, maxLength, minLength}

import scala.collection.immutable.ListMap
import scala.language.higherKinds

object UniformJourney {

  case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String)
  case class ContactDetails(firstName: String, lastName: String, email: Option[String], phoneNumber: Option[String])
  case class CtTaxForm(baReport: String, baRef: String, uprn: Option[String], address: Address, propertyContactDetails: ContactDetails)

  type AskTypes = ContactDetails :: Address :: Option[String] :: String :: NilTypes
  type TellTypes = CtTaxForm :: Long :: NilTypes

  //RestrictedStringType
  //[A-Za-z0-9\s~!&quot;@#$%&amp;'\(\)\*\+,\-\./:;&lt;=&gt;\?\[\\\]_\{\}\^&#xa3;&#x20ac;]*
  val restrictedStringTypeRegex = """[A-Za-z0-9\s\~!"@#\$&;'\(\)\*,\-\./:;<=>\?\[\\\]_\{\}\^£€]*"""

  // Email Type
  //[0-9A-Za-z'\.\-_]{1,127}@[0-9A-Za-z'\.\-_]{1,127}
  val emailAddressRegex = """[0-9A-Za-z'\.\-_]{1,127}@[0-9A-Za-z'\.\-_]{1,127}"""

  def ctTaxJourney[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[CtTaxForm] = {
    import interpreter._

    for {
      baReport <- ask[String]("ba-report", validation = baReportValidation )
      baRef <- ask[String]("ba-ref", validation = baReferenceValidation)
      upnr <-ask[Option[String]]("UPRN", validation = uprnValidation)
      address <- ask[Address]("property-address", validation = addressValidation)
      propertyContactDetails <- ask[ContactDetails]("property-contact-details", validation = propertyContactDetailValidator)
      _ <- tell[CtTaxForm]("check-answers", CtTaxForm(baReport, baRef, upnr, address, propertyContactDetails))
    } yield CtTaxForm(baReport, baRef, upnr, address, propertyContactDetails)
  }

  def baReportValidation(a: String) = {
    lengthBetween(1, 12, "ba-report.error.minLength", "ba-report.error.maxLength").apply(a) andThen (
      Rule.matchesRegex("\\d+", "ba-report.error.number").apply(_))
  }

  def baReferenceValidation(a: String) = {
    // [A-Za-z0-9\s~!&quot;@#$%&amp;'\(\)\*\+,\-\./:;&lt;=&gt;\?\[\\\]_\{\}\^&#xa3;&#x20ac;]*
    lengthBetween(1, 25, "ba-ref.error.minLength", "ba-ref.error.maxLength").apply(a) andThen ((
      Rule.matchesRegex("""[A-Za-z0-9\s\~!"@#\$&;'\(\)\*,\-\./:;<=>\?\[\\\]_\{\}\^£€]*""", "ba-ref.error.allowedChars").apply(_)))
  }

  def uprnValidation(a: Option[String]) = {
    (a match {
      case None => Validated.valid(None)
      case Some(uprn) => {
        (lengthBetween(1,12, "", "UPRN.error.maxLength" ).apply(uprn) andThen (
          Rule.matchesRegex("""\d+""", "UPRN.error.allowedChars").apply(_)
        )).map(Option(_))
      }
    })
  }

  def propertyContactDetailValidator(contactDetails: ContactDetails): Validated[ErrorTree, ContactDetails] = {

    val firstName = (lengthBetween(1, 35, "property-contact-details.firstName.minLength",
      "property-contact-details.firstName.maxLength" )
      .apply(contactDetails.firstName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "property-contact-details.firstName.allowedChars").apply(_)
    )).leftMap(_.prefixWith("firstName"))

    val lastName = (lengthBetween(1, 35, "property-contact-details.lastName.minLength",
      "property-contact-details.lastName.maxLength" )
      .apply(contactDetails.lastName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "property-contact-details.lastName.allowedChars").apply(_)
      )).leftMap(_.prefixWith("lastName"))

    val emailAddress = (contactDetails.email match {
      case None => Validated.valid(None)
      case Some(email) => {
          Rule.matchesRegex(emailAddressRegex, "property-contact-details.email.format")(email).map(Option(_))
      }
    }).leftMap(_.prefixWith("email"))

    val phoneNumber = (contactDetails.phoneNumber match {
      case None => Validated.valid(None)
      case Some(phone) => {
        PhoneNumberValidator().apply(phone).map(Option(_))
      }
    }).leftMap(_.prefixWith("phoneNumber"))


    val result = (firstName, lastName, emailAddress, phoneNumber).mapN(ContactDetails)

    result.leftMap(_.prefixWith("property-contact-details"))
  }

  def addressValidation(a: Address): Validated[ErrorTree, Address] = {

    val line1 = (lengthBetween(1, 100, "property-address.line1.minLength",
      "property-address.line1.maxLength").apply(a.line1) andThen (Rule.matchesRegex(restrictedStringTypeRegex,
      "property-address.line1.allowedChars").apply(_))).leftMap(_.prefixWith("line1"))

    val line2 = (lengthBetween(1, 100, "property-address.line2.minLength",
      "property-address.line2.maxLength").apply(a.line2) andThen (Rule.matchesRegex(restrictedStringTypeRegex,
      "property-address.line2.allowedChars").apply(_))).leftMap(_.prefixWith("line2"))

    val line3: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine("property-address.line3.maxLength", "property-address.line3.allowedChars")
      .apply(a.line3).leftMap(_.prefixWith("line3"))

    val line4: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine("property-address.line4.maxLength", "property-address.line4.allowedChars")
      .apply(a.line4).leftMap(_.prefixWith("line4"))

    val postcode = new PostcodeValidator().apply(a.postcode).leftMap(_.prefixWith("postcode"))

    val result = (line1, line2, line3, line4, postcode).mapN(Address)

    result.leftMap(_.prefixWith("property-address"))
  }

  def lengthBetween(min: Int, max: Int, minMessage: String, maxMessage:String) = new Rule[String] {
    override def apply(v1: String): Validated[ErrorTree, String] = {
        minLength[String](min, minMessage).apply(v1) andThen (maxLength[String](max, maxMessage).apply(_))
    }
  }

  def validateOptionalAddressLine(maxLenMsg: String, formatMsg: String) = new Rule[Option[String]] {
    override def apply(v1: Option[String]): Validated[ErrorTree, Option[String]] = {
      v1 match {
        case None => Validated.Valid(Option.empty[String])
        case Some(value) => {

          val res = maxLength[String](100, maxLenMsg).apply(value) andThen (Rule.matchesRegex(
            restrictedStringTypeRegex, formatMsg).apply(_))
          val res2: Validated[ ErrorTree, Option[String]] = res.map(x => Option(x))
          res2
        }
      }
    }
  }
}

