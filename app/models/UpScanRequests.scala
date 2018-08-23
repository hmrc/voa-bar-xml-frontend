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
package models

case class InitiateRequest(
  callbackUrl: String,
  maximumFileSize: Int
)

case class InitiateResponse(
  reference: String,
  uploadRequest: UploadRequest
)

case class UploadRequest(
  href: String,
  fields: UploadRequestFields
)

case class UploadRequestFields(
  `content-type`: String,
  acl: String,
  key: String,
  policy: String,
  `x-amz-algorithm`: String,
  `x-amz-credential`: String,
  `x-amz-date`: String,
  `x-amz-meta-callback-url`: String,
  `x-amz-signature`: String
)
