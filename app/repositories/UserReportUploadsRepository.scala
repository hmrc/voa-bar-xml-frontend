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

package repositories

import com.google.inject.ImplementedBy
import com.typesafe.config.ConfigException
import javax.inject.{Inject, Singleton}
import models.Error
import play.api.{Configuration, Logger}
import play.api.libs.json.Json
import reactivemongo.api.{DefaultDB, ReadPreference}
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

final case class UserReportUpload(reference: String, userId: String, userPassword: String)

object UserReportUpload {
  implicit val formats = Json.format[UserReportUpload]
  final val name = getClass.getName.toLowerCase
}

@Singleton
class UserReportUploadsReactiveRepository @Inject() (
                                   config: Configuration,
                                   mongo: () => DefaultDB
                                 )(implicit ec: ExecutionContext)
  extends ReactiveRepository[UserReportUpload, BSONObjectID](UserReportUpload.name, mongo, UserReportUpload.formats)
{
  private val indexName = UserReportUpload.name
  private val key = "reference"
  private val expireAfterSeconds = "expireAfterSeconds"
  private val ttlPath = s"${UserReportUpload.name}.timeToLiveInSeconds"
  private val ttl = config.getInt(ttlPath)
    .getOrElse(throw new ConfigException.Missing(ttlPath))
  createIndex()
  private def createIndex(): Unit = {
    collection.indexesManager.ensure(Index(Seq((key, IndexType.Hashed)), Some(indexName),
      options = BSONDocument(expireAfterSeconds -> ttl))) map {
      result => {
        Logger.debug(s"set [$indexName] with value $ttl -> result : $result")
        result
      }
    } recover {
      case e => Logger.error("Failed to set TTL index", e)
        false
    }
  }
}