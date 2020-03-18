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

package controllers
import ltbs.uniform._
import interpreters.playframework._
import javax.inject.{Inject, Singleton}
import journey.UniformJourney
import ltbs.uniform.common.web.{FormField, FormFieldStats}
import play.api.i18n.{Messages => _, _}
import play.api.mvc._
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.govukInput
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import views.html.govuk.pageChrome

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UniformController @Inject()(messagesApi: MessagesApi,
                                  pageChrome: pageChrome,
                                  govukInput: govukInput,
                                  cc: MessagesControllerComponents)(implicit ec: ExecutionContext) extends FrontendController(cc) {

  implicit val persistence: PersistenceEngine[Request[AnyContent]] =
    DebugPersistence(UnsafePersistence())

  lazy val interpreter = new AutobarsInterpreter(this, messagesApi, pageChrome, govukInput)

  def myJourney(targetId: String) = Action.async { implicit request: Request[AnyContent] =>
    import interpreter._
    import UniformJourney._


    val playProgram = ctTaxJourney[WM](create[TellTypes, AskTypes](messages(request)))

    playProgram.run(targetId, purgeStateUponCompletion = true) {
      complexForm  => Future.successful(Ok(s"${complexForm}"))
    }

  }






}
