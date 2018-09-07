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
import models.ReportStatus
import play.api.{Configuration, Logger}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import uk.gov.hmrc.mongo.{AtomicUpdate, ReactiveRepository}

import scala.concurrent.ExecutionContext
import scala.util.Try

class DefaultReportStatusRepository
(
  config: Configuration,
  defaultDB: () => DefaultDB
)(implicit ec: ExecutionContext)
  extends ReactiveRepository[ReportStatus, String](ReportStatus.name, defaultDB, ReportStatus.format)
  with AtomicUpdate[ReportStatus]
{
  private val indexName = ReportStatus.name
  private val expireAfterSeconds = "expireAfterSeconds"
  private val ttlPath = s"${ReportStatus.name}.timeToLiveInSeconds"
  private val ttl = config.getInt(ttlPath)
    .getOrElse(throw new ConfigException.Missing(ttlPath))
  createIndex()
  private def createIndex(): Unit = {
    collection.indexesManager.ensure(Index(Seq((ReportStatus.key, IndexType.Hashed)), Some(indexName),
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

  override def isInsertion(newRecordId: BSONObjectID, oldRecord: ReportStatus): Boolean =
    false
}

@Singleton
class ReportStatusRepository @Inject()
  (config: Configuration)
  (implicit ec: ExecutionContext)
{
  class DbConnection extends MongoDbConnection

  private lazy val reportStatusRepository = new DefaultReportStatusRepository(config, new DbConnection().db)

  def apply(): DefaultReportStatusRepository = {
    Try(reportStatusRepository)
      .recover{
      case ex: Throwable => {
        Logger.error(s"Error when creating DefaultReportStatusRepository\n${ex.getMessage}")
        throw ex
      }
    }.get
  }
}
