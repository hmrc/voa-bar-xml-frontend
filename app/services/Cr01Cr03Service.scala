/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import java.time.ZonedDateTime
import java.util.UUID
import com.google.inject.ImplementedBy
import connectors.ReportStatusConnector

import javax.inject.{Inject, Singleton}
import journey.UniformJourney.{Cr01Cr03Submission, Cr05Submission}
import models.{Login, Pending, ReportStatus, Submitted}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[DefaultCr01Cr03Service])
trait Cr01Cr03Service {
  def storeSubmission(submission: Cr01Cr03Submission, login: Login)(implicit hc: HeaderCarrier): Future[UUID]
  def storeSubmission(submission: Cr05Submission, login: Login)(implicit hc: HeaderCarrier): Future[UUID]
}

@Singleton
class DefaultCr01Cr03Service @Inject()(reportConnector: ReportStatusConnector)(implicit ec: ExecutionContext) extends Cr01Cr03Service {

  override def storeSubmission(submission: Cr01Cr03Submission, login: Login)(implicit hc: HeaderCarrier): Future[UUID] = {
    val submissionId = UUID.randomUUID()
    val cr01cr03Report = createReport(submission)
    val report = ReportStatus(
      id = submissionId.toString,
      created = ZonedDateTime.now(),
      baCode = Option(login.username),
      status = Option(Pending.value),
      totalReports = Option(1),
      report = Option(cr01cr03Report)
    )
    reportConnector.save(report, login).map( _ => submissionId)
  }

  def createReport(submission: Cr01Cr03Submission): JsObject = {
    val jsObj = Cr01Cr03Submission.format.writes(submission)

    Json.obj(
      "type" -> "Cr01Cr03Submission",
      "submission" -> jsObj
    )
  }

  def createReport(submission: Cr05Submission): JsObject = {
    val jsObj = Cr05Submission.format.writes(submission)

    Json.obj(
      "type" -> "Cr05Submission",
      "submission" -> jsObj
    )
  }

  override def storeSubmission(submission: Cr05Submission, login: Login)(implicit hc: HeaderCarrier): Future[UUID] = {
    val submissionId = UUID.randomUUID()
    val cr05Report = createReport(submission)
    val report = ReportStatus(
      id = submissionId.toString,
      created = ZonedDateTime.now(),
      baCode = Option(login.username),
      status = Option(Submitted.value),
      totalReports = Option(1),
      report = Option(cr05Report)
    )
    reportConnector.save(report, login).map( _ => submissionId)

  }
}
