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

package connectors

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import models.{Error, ReportStatus}
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import repositories.ReportStatusRepository
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class DefaultReportStatusConnector @Inject()(
                                      val configuration: Configuration,
                                      reportStatusRepository: ReportStatusRepository,
                                      environment: Environment)
                                     (implicit ec: ExecutionContext)
  extends ServicesConfig with ReportStatusConnector {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = configuration

  val serviceUrl = baseUrl("voa-bar")
  val baseSegment = "/voa-bar/"

  def get(userId: String): Future[Either[Error, Seq[ReportStatus]]] =
    reportStatusRepository.getByUser(userId)
  def save(reportStatus: ReportStatus): Future[Either[Error, Unit.type]] =
    reportStatusRepository.atomicSaveOrUpdate(reportStatus, true)
  def saveUserInfo(reference: String, userId: String): Future[Either[Error, Unit.type]] =
    reportStatusRepository.atomicSaveOrUpdate(userId, reference,true)
}

@ImplementedBy(classOf[DefaultReportStatusConnector])
trait ReportStatusConnector {
  def saveUserInfo(reference: String, userId: String): Future[Either[Error, Unit.type]]
  def save(reportStatus: ReportStatus): Future[Either[Error, Unit.type]]
  def get(userId: String): Future[Either[Error, Seq[ReportStatus]]]
}
