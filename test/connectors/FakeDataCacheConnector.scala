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

package connectors

import models.CacheMap
import play.api.libs.json.Format

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FakeDataCacheConnector extends DataCacheConnector {
  var captures = Map[String, Any]()

  override def save[A](cacheId: String, key: String, value: A)(implicit fmt: Format[A]): Future[CacheMap] = {
    captures = captures + (key -> value)
    Future(CacheMap(cacheId, Map()))
  }

  def getCapture(key: String): Option[Any] = captures.get(key)

  def resetCaptures() = captures = Map[String, Any]()

  override def remove(cacheId: String, key: String): Future[Boolean] = {
    captures = captures - key
    Future.successful(true)
  }

  override def fetch(cacheId: String): Future[Option[CacheMap]] = Future(Some(CacheMap(cacheId, Map())))

  override def getEntry[A](cacheId: String, key: String)(implicit fmt: Format[A]): Future[Option[A]] =
    captures.get(key) match {
      case Some(x) => Future.successful(Some(x.asInstanceOf[A]))
      case None    => Future.successful(None)
    }

  override def addToCollection[A](cacheId: String, collectionKey: String, value: A)(implicit fmt: Format[A]): Future[CacheMap] = Future(CacheMap(cacheId, Map()))

  override def removeFromCollection[A](cacheId: String, collectionKey: String, item: A)(implicit fmt: Format[A]): Future[CacheMap] =
    Future(CacheMap(cacheId, Map()))

  override def replaceInCollection[A](cacheId: String, collectionKey: String, index: Int, item: A)(implicit fmt: Format[A]): Future[CacheMap] =
    Future(CacheMap(cacheId, Map()))

  override def getEntryByField[A](field: String, value: String, key: String)(implicit fmt: Format[A]): Future[Option[A]] =
    captures.get(key) match {
      case Some(x) => Future.successful(Some(x.asInstanceOf[A]))
      case None    => Future.successful(None)
    }
}
