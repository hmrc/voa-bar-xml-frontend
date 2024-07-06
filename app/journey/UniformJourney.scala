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

package journey

import cats.data.Validated
import cats.implicits.*
import ltbs.uniform.*
import ltbs.uniform.validation.Rule.{maxLength, minLength}
import ltbs.uniform.validation.{Rule, quantString}
import models.PropertyType
import play.api.libs.json.*

import java.time.LocalDate

object UniformJourney {

  object Address { implicit val format: OFormat[Address] = Json.format[Address] }

  case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String) {

    def displayAddress = {
      val addressList = List(line1, line2) ++ List(line3, line4).flatten ++ List(postcode)
      addressList.mkString(", ")
    }
  }
  case class OtherReasonWrapper(value: String)

  object OtherReasonWrapper {
    import play.api.libs.functional.syntax.*

    implicit val otherReasonWrapperFormat: Format[OtherReasonWrapper] =
      implicitly[Format[String]].inmap(OtherReasonWrapper.apply, _.value)
  }
  object ContactDetails { implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails] }
  case class ContactDetails(firstName: String, lastName: String, email: Option[String], phoneNumber: Option[String])

  sealed trait CrSubmission

  object Cr01Cr03Submission { implicit val format: OFormat[Cr01Cr03Submission] = Json.format[Cr01Cr03Submission] }

  case class Cr01Cr03Submission(
    reasonReport: ReasonReportType,
    removalReason: Option[RemovalReasonType],
    otherReason: Option[OtherReasonWrapper],
    baReport: String,
    baRef: String,
    uprn: Option[String],
    address: Address,
    propertyContactDetails: ContactDetails,
    sameContactAddress: Boolean,
    contactAddress: Option[Address],
    effectiveDate: LocalDate,
    havePlaningReference: Boolean,
    planningRef: Option[String],
    noPlanningReference: Option[NoPlanningReferenceType],
    comments: Option[String]
  ) extends CrSubmission

  case class Cr05Common(
    baReport: String,
    baRef: String,
    effectiveDate: LocalDate,
    havePlaningReference: Boolean,
    planningRef: Option[String],
    noPlanningReference: Option[NoPlanningReferenceType]
  )

  object Cr05Common { implicit val format: OFormat[Cr05Common] = Json.format[Cr05Common] }

  case class Cr05AddProperty(
    uprn: Option[String],
    address: Address,
    propertyContactDetails: ContactDetails,
    sameContactAddress: Boolean,
    contactAddress: Option[Address]
  )
  object Cr05AddProperty { implicit val format: OFormat[Cr05AddProperty] = Json.format[Cr05AddProperty] }

  case class Cr05SubmissionBuilder(
    cr05CommonSection: Option[Cr05Common],
    existingProperties: List[Cr05AddProperty],
    proposedProperties: List[Cr05AddProperty],
    comments: Option[String]
  ) {

    def toCr05Submission: Cr05Submission =
      Cr05Submission(
        cr05CommonSection.get.baReport,
        cr05CommonSection.get.baRef,
        cr05CommonSection.get.effectiveDate,
        existingProperties,
        proposedProperties,
        cr05CommonSection.get.planningRef,
        cr05CommonSection.get.noPlanningReference,
        comments
      )
  }

  object Cr05SubmissionBuilder {
    val storageKey                                     = "ADD_PROPERTY"
    implicit val format: Format[Cr05SubmissionBuilder] = Json.format[Cr05SubmissionBuilder]
  }

  // $COVERAGE-OFF$
  object Cr05Submission {
    val storageKey                              = "CR05"
    implicit val format: Format[Cr05Submission] = Json.format[Cr05Submission]
  }

  case class Cr05Submission(
    baReport: String,
    baRef: String,
    effectiveDate: LocalDate,
    proposedProperties: Seq[Cr05AddProperty],
    existingPropertis: Seq[Cr05AddProperty],
    planningRef: Option[String],
    noPlanningReference: Option[NoPlanningReferenceType],
    comments: Option[String]
  ) extends CrSubmission {

    def asBuilder: Cr05SubmissionBuilder =
      Cr05SubmissionBuilder(
        Option(Cr05Common(baReport, baRef, effectiveDate, planningRef.isDefined, planningRef, noPlanningReference)),
        proposedProperties.toList,
        existingPropertis.toList,
        comments
      )
  }

  // $COVERAGE-ON$
  type AskTypes  =
    ReasonReportType :: RemovalReasonType :: NoPlanningReferenceType :: LocalDate ::
      YesNoType :: ContactDetails :: Address :: OtherReasonWrapper :: Option[String] :: String :: NilTypes
  type TellTypes = Cr05Common :: (Cr05AddProperty, PropertyType, Option[Int]) :: Cr05SubmissionBuilder :: Cr01Cr03Submission :: NilTypes

  // RestrictedStringType
  // [A-Za-z0-9\s~!&quot;@#$%&amp;'\(\)\*\+,\-\./:;&lt;=&gt;\?\[\\\]_\{\}\^&#xa3;&#x20ac;]*
  val restrictedStringTypeRegex = """[A-Za-z0-9\s\~!"@#\$%\+&;'\(\)\*,\-\./:;<=>\?\[\\\]_\{\}\^£€]*"""

  // More strict validation that in XML https://emailregex.com/
  val emailAddressRegex =
    """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""

  // $COVERAGE-OFF$
  def addPropertyCommon[F[_]: cats.Monad](interpreter: Language[F, TellTypes, AskTypes], cr05Common: Option[Cr05Common]): F[Cr05Common] = {
    import interpreter.*
    for {
      baReport        <- ask[String]("add-property-ba-report", validation = baReportValidation, default = cr05Common.map(_.baReport))
      baRef           <- ask[String]("add-property-ba-ref", validation = baReferenceValidation, default = cr05Common.map(_.baRef))
      effectiveDate   <- ask[LocalDate]("add-property-effective-date", default = cr05Common.map(_.effectiveDate))
      havePlanningRef <- ask[YesNoType]("add-property-have-planning-ref", default = cr05Common.map(x => booleaToYesNo(x.havePlaningReference)))
      planningRef     <- ask[String]("add-property-planning-ref", cr05Common.flatMap(_.planningRef), planningRefValidator) when (havePlanningRef == Yes)
      noPlanningRef   <- ask[NoPlanningReferenceType]("add-property-why-no-planning-ref", cr05Common.flatMap(_.noPlanningReference)) when (havePlanningRef == No)
      ctForm           = Cr05Common(baReport, baRef, effectiveDate, havePlanningRef == Yes, planningRef, noPlanningRef)
      _               <- tell[Cr05Common]("add-property-check-answers-common", ctForm)
    } yield ctForm
  }

  def addPropertyHelper[F[_]: cats.Monad](
    interpreter: Language[F, TellTypes, AskTypes],
    property: Option[Cr05AddProperty],
    propertyType: PropertyType,
    index: Option[Int]
  ): F[Cr05AddProperty] = {
    import interpreter.*
    for {
      uprn               <- ask[Option[String]]("add-property-UPRN", validation = uprnValidation, default = property.map(_.uprn))
      address            <- ask[Address]("add-property-property-address", validation = longAddressValidation("property-address"), default = property.map(_.address))
      contactDetails     <- ask[ContactDetails]("add-property-property-contact-details", property.map(_.propertyContactDetails), propertyContactDetailValidator)
      sameContactAddress <- ask[YesNoType]("add-property-same-contact-address", default = property.map(x => booleaToYesNo(x.sameContactAddress)))
      contactAddress     <- ask[Address](
                              "add-property-contact-address",
                              validation = shortAddressValidation("contact-address"),
                              default = property.flatMap(_.contactAddress)
                            ) when (sameContactAddress == No)
      ctForm              = Cr05AddProperty(uprn, address, contactDetails, sameContactAddress == Yes, contactAddress)
      _                  <- tell[(Cr05AddProperty, PropertyType, Option[Int])]("add-property-check-answers-property", ((ctForm, propertyType, index)))
    } yield ctForm
  }

  def booleaToYesNo(value: Boolean): YesNoType =
    if (value) {
      Yes
    } else {
      No
    }

  def addComments[F[_]: cats.Monad](interpreter: Language[F, TellTypes, AskTypes], default: Option[String]): F[Option[String]] = {
    import interpreter.*
    for {
      comments <- ask[Option[String]]("add-comments", validation = commentsValidation, default = Some(default))
    } yield comments
  }

  def cr05CheckYourAnswers[F[_]: cats.Monad](interpreter: Language[F, TellTypes, AskTypes])(cr05Submission: Cr05SubmissionBuilder): F[Cr05SubmissionBuilder] = {
    import interpreter.*
    for {
      _ <- tell[Cr05SubmissionBuilder]("cr05-check-answers", cr05Submission)
    } yield cr05Submission
  }

  def ctTaxJourney[F[_]: cats.Monad](interpreter: Language[F, TellTypes, AskTypes], reasonReport: ReasonReportType): F[Cr01Cr03Submission] = {
    import interpreter.*
    for {
      removalReason          <- ask[RemovalReasonType]("why-should-it-be-removed") when reasonReport == RemoveProperty
      otherReason            <- ask[OtherReasonWrapper]("other-reason", validation = otherReasonValidation) when removalReason.contains(OtherReason)
      baReport               <- ask[String]("ba-report", validation = baReportValidation)
      baRef                  <- ask[String]("ba-ref", validation = baReferenceValidation)
      uprn                   <- ask[Option[String]]("UPRN", validation = uprnValidation)
      address                <- ask[Address]("property-address", validation = longAddressValidation("property-address"))
      propertyContactDetails <- ask[ContactDetails]("property-contact-details", validation = propertyContactDetailValidator)
      sameContactAddress     <- ask[YesNoType]("same-contact-address") when reasonReport == AddProperty
      contactAddress         <- ask[Address]("contact-address", validation = shortAddressValidation("contact-address")) when (
                                  sameContactAddress.contains(No) || (reasonReport == RemoveProperty)
                                )
      effectiveDate          <- ask[LocalDate]("effective-date")
      havePlanningRef        <- ask[YesNoType]("have-planning-ref")
      planningRef            <- ask[String]("planning-ref", validation = planningRefValidator) when (havePlanningRef == Yes)
      noPlanningReference    <- ask[NoPlanningReferenceType]("why-no-planning-ref") when (havePlanningRef == No)
      comments               <- ask[Option[String]]("comments", validation = commentsValidation)
      ctForm                  = Cr01Cr03Submission(
                                  reasonReport,
                                  removalReason,
                                  otherReason,
                                  baReport,
                                  baRef,
                                  uprn,
                                  address,
                                  propertyContactDetails,
                                  sameContactAddress.contains(Yes),
                                  contactAddress,
                                  effectiveDate,
                                  havePlanningRef == Yes,
                                  planningRef,
                                  noPlanningReference,
                                  comments
                                )

      _ <- tell[Cr01Cr03Submission]("check-answers", ctForm)
    } yield ctForm
  }
  // $COVERAGE-ON$

  def otherReasonValidation(a: OtherReasonWrapper) =
    (lengthBetween(1, 32, "error.minLength", "error.maxLength").apply(a.value) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_)
    ))
      .leftMap(_.prefixWith("other-reason")).map(OtherReasonWrapper.apply)

  def baReportValidation(a: String) =
    (lengthBetween(1, 12, "error.minLength", "error.maxLength").apply(a) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_)
    ))
      .leftMap(_.prefixWith("ba-report"))

  def baReferenceValidation(a: String) =
    (lengthBetween(1, 25, "error.minLength", "error.maxLength").apply(a) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "error.allowedChars").apply(_)
    ))
      .leftMap(_.prefixWith("ba-ref"))

  def uprnValidation(a: Option[String]) =
    (a match {
      case None       => Validated.valid(None)
      case Some(uprn) =>
        (lengthBetween(1, 12, "", "error.maxLength").apply(uprn) andThen (
          Rule.matchesRegex("""\d+""", "error.allowedChars").apply(_)
        )).map(Option(_))
    }).leftMap(_.prefixWith("UPRN"))

  def commentsValidation(a: Option[String]) =
    (a match {
      case None       => Validated.valid(None)
      case Some(uprn) =>
        (lengthBetween(1, 226, "", "error.maxLength").apply(uprn) andThen (
          new journey.Iso558910Validator().apply(_)
        )).map(Option(_))
    }).leftMap(_.prefixWith("comments"))

  def planningRefValidator(planningRef: String) =
    (lengthBetween(1, 25, "error.minLength", "error.maxLength").apply(planningRef) andThen (
      new Iso558910Validator()(_)
    )).leftMap(_.prefixWith("planning-ref"))

  def propertyContactDetailValidator(contactDetails: ContactDetails): Validated[ErrorTree, ContactDetails] = {

    val firstName = (lengthBetween(1, 35, "firstName.minLength", "firstName.maxLength")
      .apply(contactDetails.firstName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "firstName.allowedChars").apply(_)
    )).leftMap(_.prefixWith("firstName"))

    val lastName = (lengthBetween(1, 35, "lastName.minLength", "lastName.maxLength")
      .apply(contactDetails.lastName) andThen (
      Rule.matchesRegex(restrictedStringTypeRegex, "lastName.allowedChars").apply(_)
    )).leftMap(_.prefixWith("lastName"))

    val emailAddress = (contactDetails.email match {
      case None        => Validated.valid(None)
      case Some(email) =>
        Rule.matchesRegex(emailAddressRegex, "email.format")(email).map(Option(_))
    }).leftMap(_.prefixWith("email"))

    val phoneNumber = (contactDetails.phoneNumber match {
      case None        => Validated.valid(None)
      case Some(phone) => PhoneNumberValidator()(phone).map(Option(_))
    }).leftMap(_.prefixWith("phoneNumber"))

    val result = (firstName, lastName, emailAddress, phoneNumber).mapN(ContactDetails.apply)

    result.leftMap(_.prefixWith("property-contact-details"))
  }

  def shortAddressValidation(errorPrefix: String)(a: Address): Validated[ErrorTree, Address] = addressValidation(35, errorPrefix)(a)
  def longAddressValidation(errorPrefix: String)(a: Address): Validated[ErrorTree, Address]  = addressValidation(100, errorPrefix)(a)

  def addressValidation(maxLen: Int, errorPrefix: String)(a: Address): Validated[ErrorTree, Address] = {

    val line1 = (lengthBetween(1, maxLen, "line1.minLength", "line1.maxLength").apply(a.line1) andThen (Rule.matchesRegex(
      restrictedStringTypeRegex,
      "line1.allowedChars"
    ).apply(_))).leftMap(_.prefixWith("line1"))

    val line2 = (lengthBetween(1, maxLen, "line2.minLength", "line2.maxLength").apply(a.line2) andThen (Rule.matchesRegex(
      restrictedStringTypeRegex,
      "line2.allowedChars"
    ).apply(_))).leftMap(_.prefixWith("line2"))

    val line3: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine(maxLen, "line3.maxLength", "line3.allowedChars")
      .apply(a.line3).leftMap(_.prefixWith("line3"))

    val line4: Validated[ErrorTree, Option[String]] = validateOptionalAddressLine(maxLen, "line4.maxLength", "line4.allowedChars")
      .apply(a.line4).leftMap(_.prefixWith("line4"))

    val postcode = new PostcodeValidator().apply(a.postcode).leftMap(_.prefixWith("postcode"))

    val result = (line1, line2, line3, line4, postcode).mapN(Address.apply)

    result.leftMap(_.prefixWith(errorPrefix))
  }

  def lengthBetween(min: Int, max: Int, minMessage: String, maxMessage: String) = new Rule[String] {

    override def apply(v1: String): Validated[ErrorTree, String] =
      minLength[String](min, minMessage).apply(v1) andThen (maxLength[String](max, maxMessage).apply(_))
  }

  def validateOptionalAddressLine(maxLen: Int, maxLenMsg: String, formatMsg: String) = new Rule[Option[String]] {

    override def apply(v1: Option[String]): Validated[ErrorTree, Option[String]] =
      v1 match {
        case None        => Validated.Valid(Option.empty[String])
        case Some(value) =>
          val res                                        = maxLength[String](maxLen, maxLenMsg).apply(value) andThen (Rule.matchesRegex(
            restrictedStringTypeRegex,
            formatMsg
          ).apply(_))
          val res2: Validated[ErrorTree, Option[String]] = res.map(x => Option(x))
          res2
      }
  }
}
