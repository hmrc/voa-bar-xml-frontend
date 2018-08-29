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

package utils

import java.nio.file.{Files, Paths}

import play.api.http.{HeaderNames, Writeable}
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{Codec, MultipartFormData}

//TODO: this class has being copied from the upscan codebase, will not be needed in play 2.6
//https://stackoverflow.com/questions/38225794/playws-post-multipart-form-data
object MultipartFormDataWritable {
  val boundary = "--------ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

  private def formatDataParts(data: Map[String, Seq[String]]) = {
    val dataParts = data
      .flatMap {
        case (key, values) =>
          values.map { value =>
            val name = s""""$key""""
            s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name\r\n\r\n$value\r\n"
          }
      }
      .mkString("")
    Codec.utf_8.encode(dataParts)
  }

  private def filePartHeader(file: FilePart[TemporaryFile]) = {
    val name     = s""""${file.key}""""
    val filename = s""""${file.filename}""""
    val contentType = file.contentType
      .map { ct =>
        s"${HeaderNames.CONTENT_TYPE}: $ct\r\n"
      }
      .getOrElse("")
    Codec.utf_8.encode(
      s"--$boundary\r\n${HeaderNames.CONTENT_DISPOSITION}: form-data; name=$name; filename=$filename\r\n$contentType\r\n")
  }

  val writeable: Writeable[MultipartFormData[TemporaryFile]] = Writeable[MultipartFormData[TemporaryFile]](
    transform = { form: MultipartFormData[TemporaryFile] =>
      formatDataParts(form.dataParts) ++
        form.files.flatMap { file =>
          val fileBytes = Files.readAllBytes(Paths.get(file.ref.file.getAbsolutePath))
          filePartHeader(file) ++ fileBytes ++ Codec.utf_8.encode("\r\n")
        } ++
        Codec.utf_8.encode(s"--$boundary--")
    },
    contentType = Some(s"multipart/form-data; boundary=$boundary")
  )
}