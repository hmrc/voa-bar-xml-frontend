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

package repositories

import models.CacheMap
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.equal
import play.api.libs.json.*
import play.api.{Configuration, Logging}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class DatedCacheMap(id: String, data: Map[String, JsValue], lastUpdated: LocalDateTime = LocalDateTime.now) {

  def asCacheMap: CacheMap = CacheMap(id, data)
}

object DatedCacheMap {

  private val localDateTimeReads: Reads[LocalDateTime] =
    Reads.at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  private val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  implicit val dateFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)
  implicit val formats: OFormat[DatedCacheMap]   = Json.format[DatedCacheMap]

  def apply(cacheMap: CacheMap): DatedCacheMap = DatedCacheMap(cacheMap.id, cacheMap.data)
}

@Singleton
class SessionRepository @Inject() (config: Configuration, mongo: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[DatedCacheMap](
    collectionName = config.get[String]("appName"),
    mongoComponent = mongo,
    domainFormat = DatedCacheMap.formats,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions().name("userAnswersExpiryIndex").expireAfter(config.get[Long]("mongodb.timeToLiveInSeconds"), SECONDS)
      )
    ),
    extraCodecs = Seq(
      Codecs.playFormatCodec(DatedCacheMap.dateFormat)
    )
  )
  with Logging {

  def upsert(cm: CacheMap): Future[Boolean] = {
    val selector = equal("id", cm.id)

    collection.findOneAndReplace(selector, DatedCacheMap(cm), FindOneAndReplaceOptions().upsert(true))
      .toFutureOption()
      .map(_ => true)
      .recover {
        case ex: Throwable =>
          logger.error("Error on saving to cache", ex)
          false
      }
  }

  def get(id: String): Future[Option[CacheMap]] =
    collection.find(equal("id", id)).first().toFutureOption().map(_.map(_.asCacheMap))

  def getByField[A](key: String, value: String): Future[Option[CacheMap]] =
    collection.find(equal(key, value)).first().toFutureOption().map(_.map(_.asCacheMap))

}
