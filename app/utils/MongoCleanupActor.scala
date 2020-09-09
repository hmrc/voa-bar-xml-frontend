/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.actor.{Actor, Props}
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.bson.DefaultBSONCommandError
import utils.MongoCleanupActor.{CleanupDone, DoCleanup}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class MongoCleanupActor @Inject()(mongo: ReactiveMongoComponent)
(implicit val ec: ExecutionContext)  extends Actor {

  val log = Logger(this.getClass)

  this.context.system.scheduler.scheduleOnce(1.minute, self, DoCleanup)

  override def receive: Receive = {
    case DoCleanup => doCleanup()
    case CleanupDone => log.info("Cleanup future successfully finished.")
  }

  private def doCleanup(): Unit = {
    import akka.pattern.pipe

    val createdIndexName = "userAnswersExpiry"
    val collectionName = "voa-bar-xml-frontend"

    val indexNotFoundErrorCode = 27
    val indexDeletedNumber = 0
    val cleanupResult =
      mongo.mongoConnector.db().indexesManager.drop(collectionName, createdIndexName).recoverWith {
        case ex @ DefaultBSONCommandError(code, errmsg, _)
          if code.contains(indexNotFoundErrorCode) && errmsg.exists(_.startsWith("index not found with name")) => {
          log.warn("The index to delete doesn't exist, check the details are correct," +
            " if so it probably has already been deleted", ex)
          Future.successful(indexDeletedNumber)
        }
        case ex: Exception => {
          log.warn("Unable to delete old index, strange error, collection.indexesManager.drop" +
            " should not fail if collection doesn't exist.", ex)
          Future.successful(indexDeletedNumber)
        }
      }.map { deleteResult =>
        if(deleteResult > 0) {
          log.info("Old index successfully deleted")
        } else {
          log.warn("Unable to delete index. Maybe it doesn't exist. Check for other exceptions.")
        }
        CleanupDone
      }

    cleanupResult pipeTo sender()
  }
}

object MongoCleanupActor {
  case object DoCleanup
  case object CleanupDone

  def props(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext): Props = {
    Props(classOf[MongoCleanupActor], mongo, ec)
  }
}
