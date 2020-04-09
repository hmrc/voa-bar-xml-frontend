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

package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formatter
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label

object SubmissionTypeFormProvider extends FormErrorHelper {

  def submissionTypeFormatter = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key) match {
      case Some(s) if optionIsValid(s) => Right(s)
      case None => produceError(key, "error.select.option")
      case _ => produceError(key, "error.unknown")
    }

    def unbind(key: String, value: String) = Map(key -> value)
  }

  def apply(): Form[String] =
    Form(single("submissionCategory" -> of(submissionTypeFormatter)))

  def options  = Seq(
    RadioItem(
      content = Text("submissionCategory.council_tax_webform"),//"create a Council Tax report"),
      id = Some("submissionCategory.council_tax_webform"),
      value = Some("council_tax_webform"),
      label = Some(Label(forAttr = Some("submissionCategory.council_tax_webform")))),
    RadioItem(
      content = Text("submissionCategory.council_tax_xml_upload"), //"Upload a file of Council Tax reports"),
      id = Some("submissionCategory.council_tax_xml_upload"),
      value = Some("council_tax_xml_upload"),
      label = Some(Label(forAttr = Some("submissionCategory.council_tax_xml_upload"))))
  )

  def optionIsValid(value: String) = options.exists(o => o.value.getOrElse("") == value)
}
