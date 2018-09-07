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

import com.typesafe.config.ConfigException
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import play.api.libs.json.{Format, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext

final case class UserReportUpload(_id: String, userId: String, userPassword: String)

object UserReportUpload {
  implicit val format = Json.format[UserReportUpload]
  final val name = classOf[UserReportUpload].getSimpleName.toLowerCase
}

@Singleton
class UserReportUploadsReactiveRepository @Inject() (
                                                   mongo: ReactiveMongoComponent,
                                                   config: Configuration
                                 )(implicit ec: ExecutionContext)
  extends ReactiveRepository[UserReportUpload, String](
    collectionName = UserReportUpload.name,
    mongo = mongo.mongoConnector.db,
    domainFormat = UserReportUpload.format,
    idFormat = implicitly[Format[String]]
    )
{
  private val indexName = UserReportUpload.name
  private val key = "_id"
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