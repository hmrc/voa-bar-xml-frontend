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

package connectors

import models.Login
import play.api.http.Status.OK
import play.api.libs.json.*
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue
import play.api.Logging
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class LoginConnector @Inject() (
  httpClientV2: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext
) extends Logging:

  private val backendBase: String = servicesConfig.baseUrl("voa-bar")
  private val loginURL: URL       = url"$backendBase/voa-bar/login"

  def doLogin(login: Login)(implicit hc: HeaderCarrier): Future[Try[Int]] =
    httpClientV2.post(loginURL)
      .withBody(Json.toJson(login))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK     => Success(OK)
          case status =>
            logger.warn("Received status of " + status + " from upstream service when logging in")
            Failure(new RuntimeException("Received status of " + status + " from upstream service when logging in"))
        }
      }
      .recover {
        case e =>
          logger.warn("Received exception " + e.getMessage + " from upstream service")
          Failure(new RuntimeException("Received exception " + e.getMessage + " from upstream service"))
      }
