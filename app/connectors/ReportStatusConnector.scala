/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.{Configuration, Environment, Logger}
import play.api.Mode.Mode
import play.api.libs.json.{JsString, JsValue}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

class ReportStatusConnector @Inject()(http: HttpClient,
                                      val configuration: Configuration,
                                      environment: Environment) extends ServicesConfig {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  val serviceUrl = baseUrl("voa-bar")
  val baseSegment = "/voa-bar/"

  def request(authorisedUsername: String)(implicit hc: HeaderCarrier): Future[Try[JsValue]] = {
    http.GET(s"$serviceUrl${baseSegment}reportstatus/${authorisedUsername}")
      .map {
        response =>
          response.status match {
            case 200 => Success(response.json)
            case status => {
              Logger.warn("Received status of " + status + " from upstream service when requesting report status")
              Failure(new RuntimeException("Received status of " + status + " from upstream service when requesting report status"))
            }
          }
      } recover {
      case e =>
        Logger.warn("Received exception " + e.getMessage + " from upstream service when requesting report status")
        Failure(new RuntimeException("Received exception " + e.getMessage + " from upstream service when requesting report status"))
    }
  }
}
