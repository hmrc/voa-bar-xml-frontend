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

import java.time.LocalDate

import cats.data.Validated
import ltbs.uniform._
import cats.implicits._
import ltbs.uniform.validation.Rule
import ltbs.uniform.validation.Rule.{maxLength, minLength}
import play.api.libs.json._


import scala.language.higherKinds

object UniformJourney {

  object Address { implicit val format = Json.format[Address] }
  case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String)
  case class OtherReasonWrapper(value : String)
  object OtherReasonWrapper {
    import play.api.libs.functional.syntax._
    implicit val otherReasonWrapperFormat: Format[OtherReasonWrapper] =
      implicitly[Format[String]].inmap(OtherReasonWrapper.apply, unlift(OtherReasonWrapper.unapply))
  }
  object ContactDetails {implicit val format = Json.format[ContactDetails] }
  case class ContactDetails(firstName: String, lastName: String, email: Option[String], phoneNumber: Option[String])
  object Cr01Cr03Submission { implicit val format = Json.format[Cr01Cr03Submission] }
  case class Cr01Cr03Submission(reasonReport: ReasonReportType, removalReason: Option[RemovalReasonType], otherReason: Option[OtherReasonWrapper],
                                baReport: String, baRef: String, uprn: Option[String], address: Address,
                                propertyContactDetails: ContactDetails,
                                sameContactAddress: Boolean, contactAddress: Option[Address],
                                effectiveDate: LocalDate, havePlaningReference: Boolean,
                                planningRef: Option[String], noPlanningReference: Option[NoPlanningReferenceType], comments: Option[String])


  case class Cr05Common(baReport: String, baRef: String, effectiveDate: LocalDate)
  object Cr05Common { implicit val format = Json.format[Cr05Common] }
  case class Cr05AddProperty(uprn: Option[String], address: Address,
                             propertyContactDetails: ContactDetails,
                             sameContactAddress: Boolean, contactAddress: Option[Address]
                             ,havePlaningReference: Boolean,
                             planningRef: Option[String], noPlanningReference: Option[NoPlanningReferenceType]
                             )
  object Cr05AddProperty { implicit val format = Json.format[Cr05AddProperty] }

  case class Cr05SubmissionBuilder(
      cr05CommonSection: Option[Cr05Common],
      propertyToBeSplit: Option[Cr05AddProperty],
      splitProperties: Option[List[Cr05AddProperty]]
  )

  object Cr05SubmissionBuilder {
    val storageKey = "ADD_PROPERTY"
    implicit val format: Format[Cr05SubmissionBuilder] = Json.format[Cr05SubmissionBuilder]
  }

  // $COVERAGE-OFF$
  object Cr05Submission {
    val storageKey = "CR05"
    implicit val format: Format[Cr05Submission] = Json.format[Cr05Submission]
    val initial = Cr05Submission(Nil, Nil)
  }

  case class Cr05Submission(
    splitProperties: Seq[Cr01Cr03Submission],
    mergeProperties: Seq[Cr01Cr03Submission]
  )
  // $COVERAGE-ON$
  type AskTypes =
    ReasonReportType :: RemovalReasonType :: NoPlanningReferenceType :: LocalDate ::
      YesNoType :: ContactDetails :: Address :: OtherReasonWrapper :: Option[String] :: String :: NilTypes
  type TellTypes = Cr05Common :: Cr05AddProperty :: Cr05Submission :: Cr01Cr03Submission :: NilTypes

  //RestrictedStringType
  //[A-Za-z0-9\s~!&quot;@#$%&amp;'\(\)\*\+,\-\./:;&lt;=&gt;\?\[\\\]_\{\}\^&#xa3;&#x20ac;]*
  val restrictedStringTypeRegex = """[A-Za-z0-9\s\~!"@#\$%\+&;'\(\)\*,\-\./:;<=>\?\[\\\]_\{\}\^£€]*"""

  // More strict validation that in XML https://emailregex.com/
  val emailAddressRegex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""


  // $COVERAGE-OFF$
  def addPropertyCommon[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[Cr05Common] = {
    import interpreter._
    for {
      baReport <- ask[String]("add-property-ba-report", validation = baReportValidation)
      baRef <- ask[String]("add-property-ba-ref", validation = baReferenceValidation)
      effectiveDate <- ask[LocalDate]("add-property-effective-date")
      ctForm = Cr05Common(baReport, baRef, effectiveDate)
      _ <- tell[Cr05Common]("add-property-check-answers-common", ctForm)
    } yield ctForm
  }

  def addPropertyHelper[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[Cr05AddProperty] = {
    import interpreter._
    for {
      uprn <- ask[Option[String]]("add-property-UPRN", validation = uprnValidation)
      address <- ask[Address]("add-property-property-address", validation = longAddressValidation("property-address"))
      propertyContactDetails <- ask[ContactDetails]("add-property-property-contact-details", validation = propertyContactDetailValidator)
      sameContactAddress <- ask[YesNoType]("add-property-same-contact-address")
      contactAddress <- ask[Address]("add-property-contact-address", validation = shortAddressValidation("contact-address")) when (
        sameContactAddress == No
        )
      havePlanningRef <- ask[YesNoType]("add-property-have-planning-ref")
      planningRef <- ask[String]("add-property-planning-ref", validation = planningRefValidator) when (havePlanningRef == Yes)
      noPlanningReference <- ask[NoPlanningReferenceType]("add-property-why-no-planning-ref") when (havePlanningRef == No)
      ctForm = Cr05AddProperty(uprn, address, propertyContactDetails,
        sameContactAddress == Yes, contactAddress, havePlanningRef == Yes, planningRef, noPlanningReference)

      _ <- tell[Cr05AddProperty]("add-property-check-answers-property", ctForm)
    } yield ctForm
  }

  def cr05CheckYourAnswers[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes])(cr05Submission: Cr05Submission): F[Cr05Submission] = {
    import interpreter._
    for {
      _ <- tell[Cr05Submission]("cr05-check-answers", cr05Submission)
    } yield cr05Submission
  }

  def ctTaxJourney[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[Cr01Cr03Submission] = {
    import interpreter._
    for {
      reasonReport <- ask[ReasonReportType]("what-is-the-reason-for-the-report")
      removalReason <- ask[RemovalReasonType]("why-should-it-be-removed") when reasonReport == RemoveProperty
      otherReason <- ask[OtherReasonWrapper]("other-reason", validation = otherReasonValidation) when removalReason.contains(OtherReason)
      baReport <- ask[String]("ba-report", validation = baReportValidation )
      baRef <- ask[String]("ba-ref", validation = baReferenceValidation)
      uprn <-ask[Option[String]]("UPRN", validation = uprnValidation)
      address <- ask[Address]("property-address", validation = longAddressValidation("property-address"))
      propertyContactDetails <- ask[ContactDetails]("property-contact-details", validation = propertyContactDetailValidator)
      sameContactAddress <- ask[YesNoType]("same-contact-address") when reasonReport == AddProperty
      contactAddress <- ask[Address]("contact-address", validation = shortAddressValidation("contact-address")) when(
        sameContactAddress.contains(No) || (reasonReport == RemoveProperty)
        )
      effectiveDate <- ask[LocalDate]("effective-date")
      havePlanningRef <- ask[YesNoType]("have-planning-ref")
      planningRef <- ask[String]("planning-ref", validation = planningRefValidator) when (havePlanningRef == Yes)
      noPlanningReference <- ask[NoPlanningReferenceType]("why-no-planning-ref") when (havePlanningRef == No)
      comments <- ask[Option[String]]("comments", validation = commentsValidation)
      ctForm = Cr01Cr03Submission(reasonReport, removalReason, otherReason,  baReport, baRef, uprn, address, propertyContactDetails,
        sameContactAddress.contains(Yes), contactAddress,
        effectiveDate, havePlanningRef == Yes, planningRef, noPlanningReference, comments)

      _ <- tell[Cr01Cr03Submission]("check-answers", ctForm)
    } yield ctForm
  }
  // $COVERAGE-ON$

  def otherReasonValidation(a: OtherReasonWrapper) = {
    (lengthBetween(1, 32, "error.minLength", "error.maxLength").apply(a.value) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_)))
      .leftMap(_.prefixWith("other-reason")).map(OtherReasonWrapper.apply)
  }
  def baReportValidation(a: String) = {
    (lengthBetween(1, 12, "error.minLength", "error.maxLength").apply(a) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_)))
      .leftMap(_.prefixWith("ba-report"))
  }

  def baReferenceValidation(a: String) = {
    (lengthBetween(1, 25, "error.minLength", "error.maxLength").apply(a) andThen ((
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_))))
      .leftMap(_.prefixWith("ba-ref"))
  }

  def uprnValidation(a: Option[String]) = {
    (a match {
      case None => Validated.valid(None)
      case Some(uprn) => {
        (lengthBetween(1,12, "", "error.maxLength" ).apply(uprn) andThen (
          Rule.matchesRegex("""\d+""", "error.allowedChars").apply(_)
        )).map(Option(_))
      }
    }).leftMap(_.prefixWith("UPRN"))
  }

  def commentsValidation(a: Option[String]) = {
    (a match {
      case None => Validated.valid(None)
      case Some(uprn) => {
        (lengthBetween(1,226, "", "error.maxLength" ).apply(uprn) andThen (
          new journey.Iso558910Validator().apply(_)
          )).map(Option(_))
      }
    }).leftMap(_.prefixWith("comments"))
  }

  def planningRefValidator(planningRef: String) = {
    (lengthBetween(1,25, "error.minLength", "error.maxLength" ).apply(planningRef) andThen(
      new Iso558910Validator()(_))
    ).leftMap(_.prefixWith("planning-ref"))

  }

  def propertyContactDetailValidator(contactDetails: ContactDetails): Validated[ErrorTree, ContactDetails] = {

    val firstName = (lengthBetween(1, 35, "firstName.minLength",
      "firstName.maxLength" )
      .apply(contactDetails.firstName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "firstName.allowedChars").apply(_)
    )).leftMap(_.prefixWith("firstName"))

    val lastName = (lengthBetween(1, 35, "lastName.minLength",
      "lastName.maxLength" )
      .apply(contactDetails.lastName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "lastName.allowedChars").apply(_)
      )).leftMap(_.prefixWith("lastName"))

    val emailAddress = (contactDetails.email match {
      case None => Validated.valid(None)
      case Some(email) => {
          Rule.matchesRegex(emailAddressRegex, "email.format")(email).map(Option(_))
      }
    }).leftMap(_.prefixWith("email"))

    val phoneNumber = (contactDetails.phoneNumber match {
      case None => Validated.valid(None)
      case Some(phone) => {
        PhoneNumberValidator().apply(phone).map(Option(_))
      }
    }).leftMap(_.prefixWith("phoneNumber"))


    val result = (firstName, lastName, emailAddress, phoneNumber).mapN(ContactDetails.apply)

    result.leftMap(_.prefixWith("property-contact-details"))
  }

  def shortAddressValidation(errorPrefix: String)(a: Address):Validated[ErrorTree, Address] = addressValidation(35, errorPrefix)(a)
  def longAddressValidation(errorPrefix: String)(a: Address): Validated[ErrorTree, Address] = addressValidation(100, errorPrefix)(a)

  def addressValidation(maxLen: Int, errorPrefix: String)(a: Address): Validated[ErrorTree, Address] = {

    val line1 = (lengthBetween(1, maxLen, "line1.minLength",
      "line1.maxLength").apply(a.line1) andThen (Rule.matchesRegex(restrictedStringTypeRegex,
      "line1.allowedChars").apply(_))).leftMap(_.prefixWith("line1"))

    val line2 = (lengthBetween(1, maxLen, "line2.minLength",
      "line2.maxLength").apply(a.line2) andThen (Rule.matchesRegex(restrictedStringTypeRegex,
      "line2.allowedChars").apply(_))).leftMap(_.prefixWith("line2"))

    val line3: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine(maxLen, "line3.maxLength", "line3.allowedChars")
      .apply(a.line3).leftMap(_.prefixWith("line3"))

    val line4: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine(maxLen, "line4.maxLength", "line4.allowedChars")
      .apply(a.line4).leftMap(_.prefixWith("line4"))

    val postcode = new PostcodeValidator().apply(a.postcode).leftMap(_.prefixWith("postcode"))

    val result = (line1, line2, line3, line4, postcode).mapN(Address.apply)

    result.leftMap(_.prefixWith(errorPrefix))
  }

  def lengthBetween(min: Int, max: Int, minMessage: String, maxMessage:String) = new Rule[String] {
    override def apply(v1: String): Validated[ErrorTree, String] = {
        minLength[String](min, minMessage).apply(v1) andThen (maxLength[String](max, maxMessage).apply(_))
    }
  }

  def validateOptionalAddressLine(maxLen: Int, maxLenMsg: String, formatMsg: String) = new Rule[Option[String]] {
    override def apply(v1: Option[String]): Validated[ErrorTree, Option[String]] = {
      v1 match {
        case None => Validated.Valid(Option.empty[String])
        case Some(value) => {

          val res = maxLength[String](maxLen, maxLenMsg).apply(value) andThen (Rule.matchesRegex(
            restrictedStringTypeRegex, formatMsg).apply(_))
          val res2: Validated[ErrorTree, Option[String]] = res.map(x => Option(x))
          res2
        }
      }
    }
  }
}

