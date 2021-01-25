/*
 * Copyright 2021 HM Revenue & Customs
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

import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.google.inject.{ImplementedBy, Inject, Singleton}
import javax.imageio.ImageIO
import models.{BillingAuthorities, ReportStatus}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.{PDDocument, PDDocumentInformation, PDPage, PDPageContentStream}
import play.api.i18n.{Lang, MessagesApi}

import scala.language.reflectiveCalls
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

@Singleton
class DefaultReceiptService @Inject() (
                                 messages: MessagesApi
                               ) extends ReceiptService {
  val font = PDType1Font.HELVETICA_BOLD
  val fontSize = 12f
  val leading = 1.5f * fontSize
  val margin = 72

  implicit val lang: Lang = Lang(Locale.UK)

  val dateFomatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' kk:mm")

  def producePDF(reportStatus: ReportStatus) = {

    val document = new PDDocument
    val page = new PDPage(PDRectangle.A4)
    document.addPage(page)

    val mediaBox = page.getMediaBox
    val startX = mediaBox.getLowerLeftX + margin
    val startY = mediaBox.getUpperRightY - margin

    setDocumentInformation(document, reportStatus)

    val contentStream = new PDPageContentStream(document, page)

    val outputStream = new ByteArrayOutputStream

    Try {
      forceClosing(document, () => {
        forceClosing(contentStream, () => {
          val height = addImage(document, page, contentStream)

          contentStream.beginText
          contentStream.newLineAtOffset(startX, startY - height - leading - 20)

          contentStream.setFont(font, fontSize + 10)
          BillingAuthorities.billingAuthorities
            .find{ case (key,_) => key == reportStatus.baCode.getOrElse("")} match {
            case Some((key, name)) =>
              contentStream.showText(s"$name - $key".toUpperCase)
              contentStream.newLineAtOffset(0f, -leading)
            case _ =>
          }

          contentStream.setFont(font, fontSize)
          wrap(page, body(reportStatus)) foreach { line =>
            contentStream.newLineAtOffset(0f, -leading)
            contentStream.showText(line)
          }
          contentStream.endText
        })

        document.save(outputStream)
      })
    } map { _ =>
      outputStream.toByteArray
    }
  }

  def forceClosing(obj: {def close(): Unit}, f: () => Unit) = {
    try {
      f()
    } finally {
      obj.close
    }
  }

  def addImage(document: PDDocument, page: PDPage, contentStream: PDPageContentStream) = {
    val mediaBox = page.getMediaBox
    val startX = mediaBox.getLowerLeftX + margin
    val startY = mediaBox.getUpperRightY - margin

    val awtImage = ImageIO.read(getClass.getResourceAsStream("/logo.png"))
    val image = LosslessFactory.createFromImage(document, awtImage)

    contentStream.drawImage(image, startX, startY - image.getHeight)

    image.getHeight
  }

  def body(reportStatus: ReportStatus) = {
    var content = messages("report.pdf.details.summary.first.line", reportStatus.filename.getOrElse("filename unavailable"), dateFomatter.format(reportStatus.created))
    content += " "

    reportStatus.status match {
      case Some(s) => content += messages(s"report.pdf.details.summary.${s.toLowerCase}")
      case _ =>
    }

    content
  }

  def wrap(page: PDPage, text: String) = {
    val mediaBox = page.getMediaBox
    val width = mediaBox.getWidth - 2 * margin

    val lines = ArrayBuffer[String]()

    var textLeft = text
    var lastSpace = -1

    while (textLeft.nonEmpty) {
      var spaceIndex = textLeft.indexOf(' ', lastSpace + 1)

      if (spaceIndex < 0) spaceIndex = textLeft.length

      var subString = textLeft.substring(0, spaceIndex)

      val size = fontSize * font.getStringWidth(subString) / 1000

      if (size > width) {
        if (lastSpace < 0) lastSpace = spaceIndex

        subString = textLeft.substring(0, lastSpace)
        lines += subString
        textLeft = textLeft.substring(lastSpace).trim
        lastSpace = -1
      } else if (spaceIndex == textLeft.length) {
        lines += textLeft
        textLeft = ""
      } else {
        lastSpace = spaceIndex
      }
    }

    lines
  }

  def author = "Valuation Office Agency"

  def title(reportStatus: ReportStatus) = {
    s"${messages("report.pdf.details.title")} ${reportStatus.baCode} - CT - ${dateFomatter.format(reportStatus.created)}"
  }

  def setDocumentInformation(document: PDDocument, reportStatus: ReportStatus): Unit = {
    val info = new PDDocumentInformation
    info.setAuthor(author)
    info.setTitle(title(reportStatus))
    document.setDocumentInformation(info)
  }
}

@ImplementedBy(classOf[DefaultReceiptService])
trait ReceiptService {
  def producePDF(reportStatus: ReportStatus): Try[Array[Byte]]
}
