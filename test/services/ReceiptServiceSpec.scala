/*
 * Copyright 2018 HM Revenue & Customs
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

import java.io.ByteArrayInputStream
import java.time.{Instant, ZoneId, ZonedDateTime}

import models._
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.text.PDFTextStripper
import org.joda.time.DateTimeUtils
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.MessagesApi

/**
  * Created by rgallet on 08/03/16.
  */
class ReceiptServiceSpec extends PlaySpec with OneAppPerSuite {

  val messages = app.injector.instanceOf[MessagesApi]
  val service = new DefaultReceiptService(messages)
  val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.systemDefault)

  "Producing a pdf" should {
    "produce a pdf - Pending" in {
      DateTimeUtils.setCurrentMillisFixed(0)
      val username = "AUser"
      val login = Login("foo", "bar")

      val baCode = "ba1221"
      val submissionId = "1234-XX"

      val reportStatus = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Pending.value))

      val data = service.producePDF(reportStatus)

      val pdf = PDDocument.load(new ByteArrayInputStream(data.get))
      pdf.getDocumentInformation.getAuthor must be("Valuation Office Agency")

      new PDFTextStripper().getText(pdf) must include ("Your file , was uploaded on 01 January 1970 at 01:00. The report is being\nverified.")

      pdf.close
      DateTimeUtils.setCurrentMillisSystem()
    }

    "produce a pdf - FAILED" in {
      DateTimeUtils.setCurrentMillisFixed(0)

      val baCode = "ba1221"
      val submissionId = "1234-XX"

      val reportStatus = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Failed.value))

      val data = service.producePDF(reportStatus)

      val pdf = PDDocument.load(new ByteArrayInputStream(data.get))

      pdf.getDocumentInformation.getAuthor must be("Valuation Office Agency")

      new PDFTextStripper().getText(pdf) must include ("Your file , was uploaded on 01 January 1970 at 01:00. The report has failed.")

      pdf.close
      DateTimeUtils.setCurrentMillisSystem()
    }

    "produce a pdf - DONE" in {
      DateTimeUtils.setCurrentMillisFixed(0)

      val baCode = "ba1221"
      val submissionId = "1234-XX"

      val reportStatus = ReportStatus(submissionId, date, baCode = Some(baCode), status = Some(Done.value))

      val data = service.producePDF(reportStatus)

      val pdf = PDDocument.load(new ByteArrayInputStream(data.get))
      pdf.getDocumentInformation.getAuthor must be("Valuation Office Agency")

      new PDFTextStripper().getText(pdf) must include ("Your file , was uploaded on 01 January 1970 at 01:00. The report was\nprocessed successfully.")

      pdf.close
      DateTimeUtils.setCurrentMillisSystem()
    }

    "have correct font" in {
      service.font must be(PDType1Font.HELVETICA_BOLD)
    }

    "have correct margin, font size and leading" in {
      service.fontSize must be(12f)
      service.leading must be(18f)
      service.margin must be(72)
    }

    "wrap text" in {
      val page = new PDPage(PDRectangle.A4)

      val mediaBox = page.getMediaBox
      val width = mediaBox.getWidth - 2 * service.margin

      val lines = service.wrap(page, "My text" * 1000)

      lines must have size (100)
      lines foreach {
        service.font.getStringWidth(_) must be <= (width * 1000)
      }
    }
  }
}
