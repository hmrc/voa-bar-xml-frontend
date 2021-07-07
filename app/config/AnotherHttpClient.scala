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

package config

import akka.actor.ActorSystem
import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.audit.http.HttpAuditing

import javax.inject.{Inject, Singleton}

/**
 * This client is here only for uk.gov.hmrc.play.partials.FormPartialRetrieverImpl. Once migrated
 * to right http client, delete this.
 * @param config
 * @param httpAuditing
 * @param wsClient
 * @param actorSystem
 */
@Singleton()
class AnotherHttpClient @Inject() (config: Configuration,
                        override val httpAuditing: HttpAuditing,
                        override val wsClient: WSClient,
                        override protected val actorSystem: ActorSystem) extends
  uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient(config, httpAuditing, wsClient, actorSystem) with uk.gov.hmrc.http.HttpClient {

}

class AnotherHttpClientModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[uk.gov.hmrc.http.HttpClient].to[AnotherHttpClient]
  )
}
