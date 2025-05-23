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

package models

import play.api.Logger
import play.api.mvc.PathBindable

sealed trait PropertyType

object PropertyType {
  case object PROPOSED extends PropertyType
  case object EXISTING extends PropertyType

  implicit val binder: PathBindable[PropertyType] = new PathBindable[PropertyType] {
    val log = Logger.apply(this.getClass)

    override def bind(key: String, value: String): Either[String, PropertyType] = value match {
      case "proposed" => Right(PROPOSED)
      case "existing" => Right(EXISTING)
      case x          =>
        log.warn(s"Unable to bind type or property : $x")
        Left("Invalid request, Unable to bind type or property")
    }

    override def unbind(key: String, value: PropertyType): String = value match {
      case PROPOSED => "proposed"
      case EXISTING => "existing"
    }
  }

}
