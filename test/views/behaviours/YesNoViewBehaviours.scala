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

package views.behaviours

import org.scalatest.AppendedClues.convertToClueful
import play.api.data.Form
import play.twirl.api.HtmlFormat

trait YesNoViewBehaviours extends QuestionViewBehaviours[Boolean]:

  def yesNoPage(createView: Form[Boolean] => HtmlFormat.Appendable, messageKeyPrefix: String): Unit =
    "behave like a page with a Yes/No question" when {
      "rendered" should {
        "contain a legend for the question" in {
          val doc     = asDocument(createView(form))
          val legends = doc.getElementsByTag("legend")
          legends.size       shouldBe 1
          legends.first.text shouldBe messages(s"$messageKeyPrefix.heading")
        }

        "contain an input for the value" in {
          val doc = asDocument(createView(form))
          assertRenderedById(doc, "value-yes")
          assertRenderedById(doc, "value-no")
        }

        "have no values checked when rendered with no form" in {
          val doc = asDocument(createView(form))
          !doc.getElementById("value-yes").hasAttr("checked") shouldBe true withClue "'value-yes' should not be checked"
          !doc.getElementById("value-no").hasAttr("checked")  shouldBe true withClue "'value-no' should not be checked"
        }

        "not render an error summary" in {
          val doc = asDocument(createView(form))
          assertNotRenderedById(doc, "error-summary_header")
        }
      }

      "rendered with a value of true" should {
        behave like answeredYesNoPage(createView, true)
      }

      "rendered with a value of false" should {
        behave like answeredYesNoPage(createView, false)
      }

      "rendered with an error" should {
        "show an error summary" in {
          val doc = asDocument(createView(form.withError(error)))
          assertRenderedById(doc, "error-summary-heading")
        }

        "show an error in the value field's label" in {
          val doc       = asDocument(createView(form.withError(error)))
          val errorSpan = doc.getElementsByClass("error-notification").first
          errorSpan.text shouldBe messages(errorMessage)
        }
      }
    }

  def answeredYesNoPage(createView: Form[Boolean] => HtmlFormat.Appendable, answer: Boolean): Unit =
    "have only the correct value checked" in {
      val doc = asDocument(createView(form.fill(answer)))
      doc.getElementById("value-yes").hasAttr("checked") shouldBe answer withClue s"value-yes.checked should be $answer"
      doc.getElementById("value-no").hasAttr("checked")  shouldBe answer withClue s"value-no.checked should not be $answer"
    }

    "not render an error summary" in {
      val doc = asDocument(createView(form.fill(answer)))
      assertNotRenderedById(doc, "error-summary_header")
    }
