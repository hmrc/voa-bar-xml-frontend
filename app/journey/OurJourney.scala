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

import play.api.libs.typedmap.{TypedKey, TypedMap}

object CTJourney {

  sealed trait Page[DATA] {
    def form: Map[String, Seq[String]] => DATA
    def url: String
    val key: TypedKey[DATA] = TypedKey[DATA]

    def nextPage(data: CtJourneyData) : Option[Page[_]]

  }

  object InputBAReport extends Page[String]{
    override def form: Map[String, Seq[String]] => String = ???
    override def url: String = "ba-report"

    override def nextPage(data: CtJourneyData): Option[Page[_]] = ???
  }

  object InputBARef extends Page[String] {
    override def form: Map[String, Seq[String]] => String = ???
    override def url: String = "ba-ref"
    override def nextPage(data: CtJourneyData): Option[Page[_]] = ???
  }

  object InputPropertyAddress extends Page[PropertyAddress] {
    override def form: Map[String, Seq[String]] => PropertyAddress = ???
    override def url: String = "property-address"
    override def nextPage(data: CtJourneyData): Option[Page[_]] = ???
  }

  val journey: Vector[Page[_]] = Vector(InputBAReport, InputBARef, InputPropertyAddress) //TODO WRONG

  val maybeNextPage = InputBAReport.nextPage(???)

  def nextPage[A](currentPage: Page[A], data: CtJourneyData) : Page[_] = {
    val gg: IndexedSeq[Page[_]] = journey
    val currentPageIndex = journey.indexOf(currentPage) //TODO - maybe not the right one
    journey(currentPageIndex + 1)

    currentPage match { //TODO Or should Page define next page? What if we need more info for decisions?
                        //TODO I can run next page in loop, to validate all steps after change
      case InputBAReport => InputBARef
      case InputBARef => InputPropertyAddress
      case InputPropertyAddress => InputPropertyAddress
    }

  }

  val superMap = TypedMap.empty
  superMap(InputBAReport.key) //TODO WRONG, can't enumerate. Impossible to serialise.

  //Super form - contain all input values, stored and overwritten all the times in mongo
  //           - case class limit only on 21, maybe to use some kind of typedMap Map[Page[A], key:[A]]
  //           - hList ???
  case class CtJourneyData(
                          baReport: Option[String],
                          baRef: Option[String],
                          propertyAddress: Option[PropertyAddress]
                          )


  // Page form classes
  case class PropertyAddress(line1: String, line2: Option[String], line3: Option[String], postcode: String)

}






