/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import javax.inject.Inject
import play.api.Logging
import play.api.libs.json._
import models.Login

import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.http.Status.OK
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class LoginConnector @Inject()(http: HttpClient,
                               val configuration: Configuration,
                               val serviceConfig: ServicesConfig)(implicit ec: ExecutionContext) extends Logging {

  val serviceUrl = serviceConfig.baseUrl("voa-bar")
  val baseSegment = "/voa-bar/"
  val jsonContentTypeHeader = ("Content-Type", "application/json")

  def send(input: Login)(implicit hc: HeaderCarrier) = sendJson(Json.toJson(input))

  def sendJson(json: JsValue)(implicit hc: HeaderCarrier): Future[Try[Int]] = {
    http.POST[JsValue, HttpResponse](s"$serviceUrl${baseSegment}login", json, Seq(jsonContentTypeHeader))
      .map {
        response =>
          response.status match {
            case OK => Success(OK)
            case status =>
              logger.warn("Received status of " + status + " from upstream service when logging in")
              Failure(new RuntimeException("Received status of " + status + " from upstream service when logging in"))
          }
      } recover {
      case e =>
        logger.warn("Received exception " + e.getMessage + " from upstream service")
        Failure(new RuntimeException("Received exception " + e.getMessage + " from upstream service"))
    }
  }

}
