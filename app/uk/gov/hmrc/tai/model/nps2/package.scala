/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.tai.model

import org.joda.time.LocalDate
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.matching.Regex

package object nps2 {

  def enumerationFormat(a: Enumeration): Format[a.Value] = new Format[a.Value] {
    def reads(json: JsValue) = JsSuccess(a.withName(json.as[String]))

    def writes(v: a.Value): JsString = JsString(v.toString)
  }

  def enumerationNumFormat(a: Enumeration): Format[a.Value] = new Format[a.Value] {
    def reads(json: JsValue) = JsSuccess(a(json.as[Int]))

    def writes(v: a.Value): JsNumber = JsNumber(v.id)
  }

  implicit val formatLocalDate: Format[LocalDate] = Format(
    new Reads[LocalDate] {
      val dateRegex: Regex = """^(\d\d)/(\d\d)/(\d\d\d\d)$""".r

      override def reads(json: JsValue): JsResult[LocalDate] = json match {
        case JsString(dateRegex(d, m, y)) =>
          JsSuccess(new LocalDate(y.toInt, m.toInt, d.toInt))
        case invalid => JsError(JsonValidationError(s"Invalid date format [dd/MM/yyyy]: $invalid"))
      }
    },
    new Writes[LocalDate] {
      val dateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd/MM/yyyy")

      override def writes(date: LocalDate): JsValue =
        JsString(dateFormat.print(date))
    }
  )
}
