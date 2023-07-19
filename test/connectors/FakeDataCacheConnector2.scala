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

package connectors

import play.api.libs.json.{Format, JsValue}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object FakeDataCacheConnector2 extends DataCacheConnector {
  var captures = Map[String, JsValue]()

  override def save[A](cacheId: String, key: String, value: A)(implicit fmt: Format[A]): Future[CacheMap] = {
    captures = captures + (key ->  fmt.writes(value))
    Future(CacheMap(cacheId, captures))
  }

  def getCapture(key: String):Option[Any] = captures.get(key)

  def resetCaptures() = captures = Map[String, JsValue]()

  override def remove(cacheId: String, key: String): Future[Boolean] = {
    captures = captures - key
    Future.successful(true)
  }

  override def fetch(cacheId: String): Future[Option[CacheMap]] = Future(Some(CacheMap(cacheId, captures)))
  def fetchMap(cacheId: String): CacheMap = CacheMap(cacheId, captures)

  override def getEntry[A](cacheId: String, key: String)(implicit fmt: Format[A]): Future[Option[A]] =
    captures.get(key) match {
      case Some(x) => Future.successful(fmt.reads(x).asOpt)
      case None => Future.successful(None)
    }

  override def addToCollection[A](cacheId: String, collectionKey: String, value: A)(implicit fmt: Format[A]): Future[CacheMap] = Future(CacheMap(cacheId, Map()))

  override def removeFromCollection[A](cacheId: String, collectionKey: String, item: A)(implicit fmt: Format[A]): Future[CacheMap] = Future(CacheMap(cacheId, Map()))

  override def replaceInCollection[A](cacheId: String, collectionKey: String, index: Int, item: A)(implicit fmt: Format[A]): Future[CacheMap] = Future(CacheMap(cacheId, Map()))

  override def getEntryByField[A](field: String, value: String, key: String)(implicit fmt: Format[A]): Future[Option[A]] = {
    captures.get(key) match {
      case Some(x) => Future.successful(Some(x.asInstanceOf[A]))
      case None => Future.successful(None)
    }
  }
}
