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

package controllers.uniform

import journey.UniformJourney.Cr05SubmissionBuilder
import ltbs.uniform.UniformMessages
import ltbs.uniform.common.web.GenericWebTell
import play.twirl.api.Html
import views.html.govuk.cr05SubmissionConfirmation

// TODO test
// $COVERAGE-OFF$
class Cr05SubmissionBuilderWebTell(cr05SubmissionConfirmation: cr05SubmissionConfirmation) extends GenericWebTell[Cr05SubmissionBuilder, Html] {

  override def render(in: Cr05SubmissionBuilder, key: String, messages: UniformMessages[Html]): Html =
    cr05SubmissionConfirmation(in, messages)
}
// $COVERAGE-ON$
