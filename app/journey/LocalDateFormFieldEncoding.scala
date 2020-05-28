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

import cats.data.NonEmptyList
import ltbs.uniform.{ErrorMsg, ErrorTree, Input}
import ltbs.uniform.common.web.{Codec, FormFieldEncoding}
import ltbs.uniform._

import scala.util.Try

object LocalDateFormFieldEncoding{
  val year = List("year")
  val month = List("month")
  val day = List("day")
}

class LocalDateFormFieldEncoding extends FormFieldEncoding[LocalDate] {
  import LocalDateFormFieldEncoding._

  override def encode(in: LocalDate): Input = {
    Map(
      year -> List(in.getYear.toString),
      month -> List(in.getMonthValue.toString),
      day -> List(in.getDayOfMonth.toString)
    )
  }

  override def decode(out: Input): Either[ErrorTree, LocalDate] = {
    val yearValue = out.get(year).flatMap(_.headOption).map(_.trim).filter(_ != "")
    val monthValue = out.get(month).flatMap(_.headOption).map(_.trim).filter(_ != "")
    val dayValue = out.get(day).flatMap(_.headOption).map(_.trim).filter(_ != "")

    val validation: Either[ErrorTree, LocalDate] = (dayValue, monthValue, yearValue) match {
      case (None, None, None) => Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.mandatory"))
        .append(ErrorMsg("day")).append(ErrorMsg("month")).append(ErrorMsg("year"))
      ))

      case (None, None, _) => Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.mandatory.dayMonth"))
        .append(ErrorMsg("day")).append(ErrorMsg("month"))
      ))

      case (None, _, None) => Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.mandatory.dayYear"))
        .append(ErrorMsg("day")).append(ErrorMsg("year"))
      ))

      case (_, None, None) => Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.mandatory.monthYear"))
        .append(ErrorMsg("month")).append(ErrorMsg("year"))
      ))

      case (None, _, _) => Left(ErrorTree.one(
        NonEmptyList.one(ErrorMsg("error.mandatory.day")).append(ErrorMsg("day"))
      ))

      case (_, None, _) =>Left(ErrorTree.one(
        NonEmptyList.one(ErrorMsg("error.mandatory.month")).append(ErrorMsg("month"))
      ))
      case (_, _, None) => Left(ErrorTree.one(
        NonEmptyList.one(ErrorMsg("error.mandatory.year")).append(ErrorMsg("year"))
      ))

      case (Some(x), Some(y), Some(z)) => {
        validateDateField(x, y, z)
      }
    }
    validation
  }

  val testRegex = """\d+""".r.pattern
  val earliestEffectiveDate = LocalDate.of(1993, 4, 1)

  def validateDateField(day: String, month: String, year: String): Either[ErrorTree, LocalDate] = {
    for {
      day <- validateDay(day).right
      month <- validateMont(month).right
      year <- validateYear(year).right
      finalDate <- {
        Try (
          LocalDate.of(year, month, day)
        ).toEither.left.map(_ => ErrorTree.one(
          NonEmptyList.one(ErrorMsg("error.mandatory"))
            .append(ErrorMsg("day"))
            .append(ErrorMsg("month"))
            .append(ErrorMsg("year"))
        )).right.flatMap { date =>
          if(date.isBefore(earliestEffectiveDate) || date.isAfter(LocalDate.now())) {
            Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.year.range"))
              .append(ErrorMsg("day")).append(ErrorMsg("month")).append(ErrorMsg("year"))))
          }else {
            Right(date)
          }
        }
      }
    }yield {
      finalDate
    }
  }

  def validateDay(day: String): Either[ErrorTree, Int] = {
    if(testRegex.matcher(day).matches()) {
      val intDay = day.toInt
      if(intDay > 31 || intDay < 1) {
        Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.day.range"))
          .append(ErrorMsg("day"))))
      }else {
        Right(intDay)
      }
    }else {
      Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.day.number"))
        .append(ErrorMsg("day"))))
    }
  }

  def validateMont(month: String): Either[ErrorTree, Int] = {
    if(testRegex.matcher(month).matches()) {
      val intMonth = month.toInt
      if(intMonth > 12 | intMonth < 1) {
        Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.month.range"))
          .append(ErrorMsg("month"))))
      }else {
        Right(intMonth)
      }
    }else {
      Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.month.number"))
        .append(ErrorMsg("month"))))
    }
  }

  def validateYear(year: String): Either[ErrorTree, Int] = {
    if(testRegex.matcher(year).matches()) {
      val intYear = year.toInt
      if(intYear < 1993 || intYear > LocalDate.now().getYear() ) {
        Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.year.range"))
          .append(ErrorMsg("year"))))
      }else {
        Right(intYear)
      }
    }else {
      Left(ErrorTree.one(NonEmptyList.one(ErrorMsg("error.year.number"))
        .append(ErrorMsg("year"))))
    }
  }


}
