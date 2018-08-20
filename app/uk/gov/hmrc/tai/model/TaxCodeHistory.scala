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

package uk.gov.hmrc.tai.model

import org.joda.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.time.TaxYearResolver

case class TaxCodeHistory(nino: String, taxCodeRecord: Seq[TaxCodeRecord]) {
  def operatedTaxCodeRecords: Seq[TaxCodeRecord] = taxCodeRecord.filter(_.operatedTaxCode)
}

object TaxCodeHistory {

  implicit val format = Json.format[TaxCodeHistory]
  implicit val dateTimeOrdering: Ordering[LocalDate] = Ordering.fromLessThan(_ isAfter _)

  def removeAnnualCodingDuplicates(taxCodeRecords: Seq[TaxCodeRecord]):Seq[TaxCodeRecord] = {
    val currentYear = TaxYearResolver.startOfCurrentTaxYear

    val (currentTaxYearRecords, preTaxYearRecords) = taxCodeRecords.sortBy(_.dateOfCalculation).partition(_.dateOfCalculation isAfter currentYear)

    val latestTaxCodeRecord:TaxCodeRecord = preTaxYearRecords.size match {
      case(1) => preTaxYearRecords(0)
      case _ => preTaxYearRecords.sortBy(_.dateOfCalculation).head
    }

    Seq(latestTaxCodeRecord) ++ currentTaxYearRecords
  }

}


