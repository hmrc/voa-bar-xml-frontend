/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import models.requests.DataRequest
import play.api.mvc.Results.Unauthorized
import play.api.mvc.{ActionFilter, MessagesControllerComponents, Result}
import views.html.unauthorised

import scala.concurrent.{ExecutionContext, Future}

class AuthAction @Inject() (cc: MessagesControllerComponents,
                            appConfig: FrontendAppConfig,
                            unauthorised: unauthorised)(implicit val executionContext: ExecutionContext)
  extends ActionFilter[DataRequest]{

  override protected def filter[A](request: models.requests.DataRequest[A]): Future[Option[Result]] = {
    if(request.userAnswers.login.isEmpty) {
      val messages = cc.messagesApi.preferred(request)
      Future.successful(Some(Unauthorized(unauthorised(appConfig)(request, messages))))
    } else {
      Future.successful(None)
    }
  }

}
