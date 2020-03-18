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

import ltbs.uniform._
import cats.implicits._
import ltbs.uniform.interpreters.cli.{AskCli, CliInterpreter}
import ltbs.uniform.validation.Rule

import scala.language.higherKinds

object UniformJourney extends App {

  case class Address(line1: String, line2: String, line3: Option[String], line4: Option[String], postcode: String)
  case class ComplexForm(baReport: String, baRef: String, address: Address)

  type AskTypes =  String :: NilTypes
  type TellTypes = Long :: NilTypes


  def ctTaxJourney[F[_] : cats.Monad](interpreter: Language[F, TellTypes, AskTypes]): F[ComplexForm] = {
    import interpreter._

    for {
      baReport <- interpreter.ask[String]("ba-report", validation = baReportValidation )
      baRef <- ask[String]("ba-ref", validation = baReferenceValidation)
      address <- ask[String]("address")
    } yield ComplexForm(baReport, baRef, Address(address, "", None, None, ""))
  }

  def baReportValidation(a: String) = {
    Rule.lengthBetween[String](1,12).apply(a) andThen (
      Rule.matchesRegex("\\d+", "ba-report.error.number").apply(_))
  }

  def baReferenceValidation(a: String) = {
    Rule.maxLength[String](25).apply(a) andThen ((Rule.matchesRegex("[a-zA-Z0-9 \\-']+").apply(_)))
  }



  /*
  (
  NonEmptyList(
    List()),
    NonEmptyList(
      ErrorMsg(
        minLength,
        WrappedArray(0, 1)
      )
    )
  )
   */


  def gg(id: String): Either[String, Address] = {
    Console.println(s"id for address : ${id}")
    Right(Address("aaa", "ggg", None, None, "Postcode"))
  }

  implicit val askCliAddress: AskCli[Address] =  {
    new AskCli[Address] {
      override def apply(in: String, validation: Rule[Address]): Address = {
        val line1 = CliInterpreter.askString("line1", Rule.nonEmpty)
        val line2 = CliInterpreter.askString("line2", Rule.nonEmpty)
        val postcode =  CliInterpreter.askString("line1", Rule.nonEmpty)
        Address(line1, line2, None, None, postcode)
      }
    }
  }

  val interpreter = new CliInterpreter[TellTypes, AskTypes]()

  ctTaxJourney(interpreter)


}

