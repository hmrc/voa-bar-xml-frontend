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

import forms.FeedbackForm
import models.FeedbackAuditEvent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.config.AuditingConfig
import uk.gov.hmrc.play.audit.http.connector.{AuditChannel, AuditConnector, DatastreamMetrics}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

/**
  * @author Yuriy Tumakha
  */
@Singleton
class AuditService @Inject() (
  val auditingConfig: AuditingConfig,
  val auditChannel: AuditChannel,
  val datastreamMetrics: DatastreamMetrics
)(implicit val ec: ExecutionContext
) extends AuditConnector {

  def sendFeedback(form: FeedbackForm)(implicit hc: HeaderCarrier): Unit = {
    val auditType = if (form.afterSubmission) "SurveySatisfaction" else "SurveyFeedback"
    val event     = FeedbackAuditEvent(form.rating, form.comments, form.afterSubmission, form.submissionId)

    sendExplicitAudit(auditType, event)
  }

}
