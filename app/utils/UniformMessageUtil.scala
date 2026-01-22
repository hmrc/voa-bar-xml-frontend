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

package utils

import ltbs.uniform.UniformMessages
import play.api.i18n.Messages
import play.twirl.api.Html

object UniformMessageUtil {

  def getViewUniformMessages(implicit messages: Messages): UniformMessages[Html] =
    new UniformMessages[Html] {
      override def get(key: String, args: Any*): Option[Html] = Option(Html(messages(key, args)))

      override def list(key: String, args: Any*): List[Html] = Nil
    }

}
