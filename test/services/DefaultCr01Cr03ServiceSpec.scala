/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import connectors.ReportStatusConnector
import journey.AddProperty
import journey.UniformJourney.{Address, ContactDetails, Cr01Cr03Submission, Cr05Submission}
import models.{Login, ReportStatus}
import org.mockito.captor.ArgCaptor
import org.mockito.scalatest.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DefaultCr01Cr03ServiceSpec extends PlaySpec with MockitoSugar {

  val BA_REF = "BA2020"

  "DefaultCr01Cr03ServiceSpec" should {
    "Create CR05 report and store in backend" in {
      val connector = mock[ReportStatusConnector]
      when(connector.save(any[ReportStatus],any[Login])(any[HeaderCarrier])).thenReturn(Future.successful(Right(())))

      val captor = ArgCaptor[ReportStatus]
      val service = new DefaultCr01Cr03Service(connector)

      val submission = Cr05Submission(BA_REF, "baRef", LocalDate.of(2020,2,2),
        Seq(), Seq(), Some("planningRef"), None, Some("comment")
      )

      service.storeSubmission(submission, Login(BA_REF, "xxx", None))(HeaderCarrier())

      verify(connector).save(captor, any[Login])(any[HeaderCarrier])

      captor.value must not be null
      captor.value.baCode.value mustBe(BA_REF)
      captor.value.report.value.value.get("type").value mustBe(JsString("Cr05Submission"))

    }

    "Create CR01 and CR03 report and store in backend" in {
      val connector = mock[ReportStatusConnector]
      when(connector.save(any[ReportStatus],any[Login])(any[HeaderCarrier])).thenReturn(Future.successful(Right(())))

      val captor = ArgCaptor[ReportStatus]
      val service = new DefaultCr01Cr03Service(connector)

      val submission = Cr01Cr03Submission(AddProperty,None, None, "baReport", BA_REF, Option("112313"),
        Address("line1", "line2",None, None, "BN11 4EF"),
        ContactDetails("first name", "last name", None, None),
        true,None, LocalDate.of(2020,2,2), true, Some("1231231"),None, Some("comment")
      )

      service.storeSubmission(submission, Login(BA_REF, "xxx", None))(HeaderCarrier())

      verify(connector).save(captor, any[Login])(any[HeaderCarrier])

      captor.value must not be null
      captor.value.baCode.value mustBe(BA_REF)
      captor.value.report.value.value.get("type").value mustBe(JsString("Cr01Cr03Submission"))
    }

  }
}