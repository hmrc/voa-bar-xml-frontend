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

package config

import filters.VoaBarAuditFilter
import play.api.{Configuration, Environment, Logging}
import play.api.inject.{Binding, Module}
import uk.gov.hmrc.play.bootstrap.frontend.FrontendModule
import uk.gov.hmrc.play.bootstrap.filters.AuditFilter

/**
  * Custom frontend module. It bind everything from FrontendModule but change binding for FrontendAuditFilter.
  * We use custom implementation which prevent logging of sensitive fields.
  */
class VoaBarFrontendModule extends Module with Logging {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    logger.warn("Binding audit filter")

    new FrontendModule().bindings(environment, configuration)
    .map { binding =>
      if(binding.key.clazz.equals(classOf[AuditFilter])) {
        bind[AuditFilter].to[VoaBarAuditFilter]
      }else {
        binding
      }
    }

  }
}
