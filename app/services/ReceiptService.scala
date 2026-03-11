/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.{BillingAuthorities, ReportStatus}
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.{PDType1Font, Standard14Fonts}
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.{PDDocument, PDDocumentInformation, PDPage, PDPageContentStream}
import play.api.i18n.{Lang, MessagesApi}

import java.io.{ByteArrayOutputStream, Closeable}
import java.util.Locale
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.util.Try

@Singleton
class DefaultReceiptService @Inject() (
  messages: MessagesApi
) extends ReceiptService:

  val font: PDType1Font = PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
  val fontSize: Float   = 12f
  val leading: Float    = 1.5f * fontSize
  val margin: Int       = 72

  private val author = "Valuation Office"

  given Lang = Lang(Locale.UK)

  def producePDF(reportStatus: ReportStatus): Try[Array[Byte]] =
    val document = PDDocument()
    val page     = PDPage(PDRectangle.A4)
    document.addPage(page)

    val mediaBox = page.getMediaBox
    val startX   = mediaBox.getLowerLeftX + margin
    val startY   = mediaBox.getUpperRightY - margin

    setDocumentInformation(document, reportStatus)

    val contentStream = PDPageContentStream(document, page)
    val outputStream  = ByteArrayOutputStream()

    Try {
      forceClosing(
        document,
        () => {
          forceClosing(
            contentStream,
            () => {
              val height = addImage(document, page, contentStream)

              contentStream.beginText()
              contentStream.newLineAtOffset(startX, startY - height - leading - 20)

              contentStream.setFont(font, fontSize + 10)
              BillingAuthorities.billingAuthorities
                .find { case (key, _) => key == reportStatus.baCode.getOrElse("") } match
                case Some((key, name)) =>
                  contentStream.showText(s"$name - $key".toUpperCase)
                  contentStream.newLineAtOffset(0f, -leading)
                case _                 =>

              contentStream.setFont(font, fontSize)
              wrap(page, body(reportStatus)) foreach { line =>
                contentStream.newLineAtOffset(0f, -leading)
                contentStream.showText(line)
              }
              contentStream.endText()
            }
          )

          document.save(outputStream)
        }
      )
    } map { _ =>
      outputStream.toByteArray
    }

  private def forceClosing(obj: Closeable, f: () => Unit): Unit =
    try f()
    finally obj.close()

  private def addImage(document: PDDocument, page: PDPage, contentStream: PDPageContentStream) =
    val mediaBox = page.getMediaBox
    val startX   = mediaBox.getLowerLeftX + margin
    val startY   = mediaBox.getUpperRightY - margin
    val awtImage = ImageIO.read(getClass.getResourceAsStream("/logo.png"))
    val image    = LosslessFactory.createFromImage(document, awtImage)
    contentStream.drawImage(image, startX, startY - image.getHeight)
    image.getHeight

  private def body(reportStatus: ReportStatus): String =
    var content = messages("report.pdf.details.summary.first.line", reportStatus.filename.getOrElse("filename unavailable"), reportStatus.formattedCreatedLong)
    content += " "
    reportStatus.status match
      case Some(s) => content += messages(s"report.pdf.details.summary.${s.toLowerCase}")
      case _       =>
    content

  def wrap(page: PDPage, text: String): mutable.Seq[String] =
    val mediaBox  = page.getMediaBox
    val width     = mediaBox.getWidth - 2 * margin
    val lines     = ArrayBuffer[String]()
    var textLeft  = text
    var lastSpace = -1

    while (textLeft.nonEmpty)
      var spaceIndex = textLeft.indexOf(' ', lastSpace + 1)
      if (spaceIndex < 0) spaceIndex = textLeft.length
      var subString  = textLeft.substring(0, spaceIndex)
      val size       = fontSize * font.getStringWidth(subString) / 1000
      if size > width then
        if lastSpace < 0 then lastSpace = spaceIndex
        subString = textLeft.substring(0, lastSpace)
        lines += subString
        textLeft = textLeft.substring(lastSpace).trim
        lastSpace = -1
      else if spaceIndex == textLeft.length then
        lines += textLeft
        textLeft = ""
      else
        lastSpace = spaceIndex
    lines

  private def title(reportStatus: ReportStatus) =
    s"${messages("report.pdf.details.title")} ${reportStatus.baCode} - CT - ${reportStatus.formattedCreatedLong}"

  private def setDocumentInformation(document: PDDocument, reportStatus: ReportStatus): Unit =
    val info = PDDocumentInformation()
    info.setAuthor(author)
    info.setTitle(title(reportStatus))
    document.setDocumentInformation(info)

@ImplementedBy(classOf[DefaultReceiptService])
trait ReceiptService:
  def producePDF(reportStatus: ReportStatus): Try[Array[Byte]]
