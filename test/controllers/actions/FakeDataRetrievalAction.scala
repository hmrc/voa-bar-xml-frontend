/*
 * Copyright 2024 HM Revenue & Customs
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

import models.CacheMap
import play.api.mvc.{AnyContent, BodyParser, Request}
import models.requests.OptionalDataRequest
import play.api.test.Helpers
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class FakeDataRetrievalAction(cacheMapToReturn: Option[CacheMap]) extends DataRetrievalAction {

  override protected def transform[A](request: Request[A]): Future[OptionalDataRequest[A]] = cacheMapToReturn match {
    case None           => Future(OptionalDataRequest(request, "id", None))
    case Some(cacheMap) => Future(OptionalDataRequest(request, "id", Some(new UserAnswers(cacheMap))))
  }

  override def parser: BodyParser[AnyContent] = Helpers.stubBodyParser(AnyContent(""))

  override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}
