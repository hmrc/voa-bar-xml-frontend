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

package views

import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import base.SpecBase
import com.typesafe.config.ConfigFactory
import config.FrontendAppConfig
import play.api.Configuration
import play.api.i18n.Lang
import play.api.test.Injecting
import uk.gov.hmrc.govukfrontend.views.html.components._
import uk.gov.hmrc.govukfrontend.views.html.helpers.formWithCSRF
import uk.gov.hmrc.govukfrontend.views.html.layouts.{govukLayout, govukTemplate}
import uk.gov.hmrc.hmrcfrontend.views.html.components.HmrcFooter
import uk.gov.hmrc.hmrcfrontend.views.html.helpers.{HmrcFooterItems, HmrcTrackingConsentSnippet, hmrcStandardFooter}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.config.AccessibilityStatementConfig
import views.html.components.{confirmation_detail_panel, confirmation_status_panel}
import views.html.govuk.{head, scripts}
import views.html.{add_to_list, confirmation, councilTaxUpload, error_template, login, main_no_container, reportStatus, task_list, welcome}

trait ViewSpecBase extends SpecBase with Injecting {

  implicit def lang = Lang(Locale.UK)

  def asDocument(html: Html): Document = Jsoup.parse(html.toString())

  def assertEqualsMessage(doc: Document, cssSelector: String, expectedMessageKey: String) =
    assertEqualsValue(doc, cssSelector, messages(expectedMessageKey))

  def assertEqualsValue(doc : Document, cssSelector : String, expectedValue: String) = {
    val elements = doc.select(cssSelector)

    if(elements.isEmpty) throw new IllegalArgumentException(s"CSS Selector $cssSelector wasn't rendered.")

    //<p> HTML elements are rendered out with a carriage return on some pages, so discount for comparison
    assert(elements.first().html().replace("\n", "") == expectedValue)
  }

  def assertPageTitleEqualsMessage(doc: Document, expectedMessageKey: String, args: Any*) = {
    val headers = doc.getElementsByTag("h1")
    headers.size mustBe 1
    headers.first.text.replaceAll("\u00a0", " ") contains messages(expectedMessageKey, args:_*).replaceAll("&nbsp;", " ")
  }

  def assertContainsText(doc:Document, text: String) = assert(doc.toString.contains(text), "\n\ntext " + text + " was not rendered on the page.\n")

  def assertContainsMessages(doc: Document, expectedMessageKeys: String*) = {
    for (key <- expectedMessageKeys) assertContainsText(doc, messages(key))
  }

  def assertRenderedById(doc: Document, id: String) = {
    assert(doc.getElementById(id) != null, "\n\nElement " + id + " was not rendered on the page.\n")
  }

  def assertNotRenderedById(doc: Document, id: String) = {
    assert(doc.getElementById(id) == null, "\n\nElement " + id + " was rendered on the page.\n")
  }

  def assertRenderedByCssSelector(doc: Document, cssSelector: String) = {
    assert(!doc.select(cssSelector).isEmpty, "Element " + cssSelector + " was not rendered on the page.")
  }

  def assertNotRenderedByCssSelector(doc: Document, cssSelector: String) = {
    assert(doc.select(cssSelector).isEmpty, "\n\nElement " + cssSelector + " was rendered on the page.\n")
  }

  def assertContainsLabel(doc: Document, forElement: String, expectedText: String, expectedHintText: Option[String] = None) = {
    val labels = doc.getElementsByAttributeValue("for", forElement)
    assert(labels.size == 1, s"\n\nLabel for $forElement was not rendered on the page.")
    val label = labels.first
    assert(label.text() == expectedText, s"\n\nLabel for $forElement was not $expectedText")

    if (expectedHintText.isDefined) {
      assert(label.getElementsByClass("form-hint").first.text == expectedHintText.get,
        s"\n\nLabel for $forElement did not contain hint text $expectedHintText")
    }
  }

  def assertElementHasClass(doc: Document, id: String, expectedClass: String) = {
    assert(doc.getElementById(id).hasClass(expectedClass), s"\n\nElement $id does not have class $expectedClass")
  }

  def assertContainsRadioButton(doc: Document, id: String, name: String, value: String, isChecked: Boolean) = {
    assertRenderedById(doc, id)
    val radio = doc.getElementById(id)
    assert(radio.attr("name") == name, s"\n\nElement $id does not have name $name")
    assert(radio.attr("value") == value, s"\n\nElement $id does not have value $value")
    isChecked match {
      case true => assert(radio.attr("checked") == "checked", s"\n\nElement $id is not checked")
      case _ => assert(!radio.hasAttr("checked") && radio.attr("checked") != "checked", s"\n\nElement $id is checked")
    }
  }

  def createLoginView(): login = {
    new login(
      createMain_template(),
      new formWithCSRF(),
      uk.gov.hmrc.govukfrontend.views.html.components.GovukInput,
      uk.gov.hmrc.govukfrontend.views.html.components.GovukButton,
      uk.gov.hmrc.govukfrontend.views.html.components.GovukErrorSummary
    )
  }

  def createWelcomeView(): welcome = {
    new welcome(
      createMain_template()
    )
  }

  def createTaskListView(): task_list = {
    new task_list(
      createMain_template()
    )
  }

  def createAddToListView(): add_to_list = {
    new add_to_list(
      createMain_template(),
      new formWithCSRF(),
      new GovukButton(),
      GovukRadios,
      new GovukErrorSummary()
    )
  }

  def createUploadView(): councilTaxUpload = {
    new councilTaxUpload(
      createMain_template(),
      new govukFileUpload(GovukErrorMessage, GovukHint, GovukLabel),
      new govukErrorSummary()
    )
  }

  def createReportStatusView(): reportStatus = {
    new reportStatus(
      createMain_no_container_template(),
      new govukTable()
    )
  }

  def createConfirmationView(): confirmation = {
    new confirmation(
      createMain_template(),
      inject[confirmation_status_panel],
      inject[confirmation_detail_panel],
      new govukSummaryList()
    )
  }

  def createErrorTemplateView(): error_template = {
    new error_template(
      createMain_template()
    )
  }

  def createMain_template(): views.html.govuk.main_template = {
    new views.html.govuk.main_template(
      createGovukLayout(),
      create_head(),
      create_scripts(),
      new govukPhaseBanner(new govukTag()),
      new views.html.components.siteHeader(uk.gov.hmrc.govukfrontend.views.html.components.GovukHeader),
      new views.html.components.siteFooter(uk.gov.hmrc.govukfrontend.views.html.components.GovukFooter),
      new govukBackLink(),
      new govukSummaryList(),
      inject[AccessibilityStatementConfig]
    )
  }

  def createMain_no_container_template(): views.html.main_no_container = {
    new main_no_container(
      createGovukLayout(),
      create_head(),
      create_scripts(),
      new govukPhaseBanner(new govukTag()),
      new views.html.components.siteHeader(uk.gov.hmrc.govukfrontend.views.html.components.GovukHeader),
      new govukTemplate(new GovukHeader(), new GovukFooter(), new GovukSkipLink()),
      new govukBackLink(),
      new govukSummaryList(),
      inject[AccessibilityStatementConfig],
      new hmrcStandardFooter(new HmrcFooter(), new HmrcFooterItems(new uk.gov.hmrc.hmrcfrontend.config.AccessibilityStatementConfig(Configuration(ConfigFactory.load))))
    )
  }

  def create_head(): head = {
    val conf = new uk.gov.hmrc.hmrcfrontend.config.TrackingConsentConfig(Configuration())
    val trackingConsentSnippet = new HmrcTrackingConsentSnippet(conf)
    new head(trackingConsentSnippet)
  }

  def create_scripts(): scripts = {
    val config = ConfigFactory.load
    val configuration = Configuration(config)
    val serviceConfig = new ServicesConfig(configuration, new RunMode(configuration, play.api.Mode.Test))
    val appConfig = new FrontendAppConfig(configuration, serviceConfig)
    new scripts(appConfig, configuration)
  }

  def createGovukLayout(): govukLayout = {
    new govukLayout(uk.gov.hmrc.govukfrontend.views.html.components.GovukTemplate, GovukHeader, GovukFooter, GovukBackLink)
  }

  def createSessionTimeoutView(): views.html.session_timeout =
    new views.html.session_timeout(
      createMain_template()
    )



}
