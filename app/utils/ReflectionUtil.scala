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

import scala.compiletime.summonAll
import scala.deriving.Mirror

/**
  * @author Yuriy Tumakha
  */
object ReflectionUtil {

  inline def findAllSubTypeNames[A](using m: Mirror.SumOf[A]): Seq[String] =
    findAllSubTypes.map(_.getClass.getSimpleName.stripSuffix("$"))

  inline def findAllSubTypes[A](using m: Mirror.SumOf[A]): Seq[A] =
    summonAll[Tuple.Map[m.MirroredElemTypes, ValueOf]]
      .productIterator
      .map {
        case v: ValueOf[A @unchecked] => v.value
      }.toSeq

}
