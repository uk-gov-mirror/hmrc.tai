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

package uk.gov.hmrc.tai.service

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.tai.connectors.TaxCodeChangeConnector
import uk.gov.hmrc.tai.model.api.TaxCodeChange
import uk.gov.hmrc.tai.model._
import uk.gov.hmrc.tai.util.{TaiConstants, TaxCodeRecordConstants}
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class TaxCodeChangeServiceImplSpec extends PlaySpec with MockitoSugar with TaxCodeRecordConstants {

  "hasTaxCodeChanged" should {
    "return true" when {

      "the tax code has been operated" when {

        "there has been one daily tax code change in the year after Annual Coding" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = newCodeDate, NonAnnualCode),
              TaxCodeRecord(taxCode = "1080L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = previousCodeDate, AnnualCode)
            )
          )

          val mockConnector = mock[TaxCodeChangeConnector]
          when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

          val service = new TaxCodeChangeServiceImpl(mockConnector)
          Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }

        "there has been more than one daily tax code change in the year" in {
          val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
          val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(1)
          val testNino = randomNino

          val taxCodeHistory = TaxCodeHistory(
            nino = testNino.withoutSuffix,
            taxCodeRecord = Seq(
              TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = newCodeDate, NonAnnualCode),
              TaxCodeRecord(taxCode = "1080L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = previousCodeDate, NonAnnualCode)
            )
          )

          val mockConnector = mock[TaxCodeChangeConnector]
          when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

          val service = new TaxCodeChangeServiceImpl(mockConnector)
          Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual true
        }
      }
    }

    "return false" when {
      "one daily tax code is returned without an Annual coding tax code" in {
        val testNino = randomNino
        val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = newCodeDate, NonAnnualCode))
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)
        Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }

      "there has been one tax code change in the year but it has not been operated" in {
        val newCodeDate = TaxYearResolver.startOfCurrentTaxYear.plusMonths(2)
        val previousCodeDate = TaxYearResolver.startOfCurrentTaxYear
        val testNino = randomNino

        val taxCodeHistory = TaxCodeHistory(
          nino = testNino.withoutSuffix,
          taxCodeRecord = Seq(
            TaxCodeRecord(taxCode = "1000L", employerName = "Employer 1", operatedTaxCode = false, dateOfCalculation = newCodeDate, NonAnnualCode),
            TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = previousCodeDate, AnnualCode)
          )
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)
        Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }

      "there has not been a tax code change in the year" in {
        val annualCodeDate = TaxYearResolver.startOfCurrentTaxYear
        val testNino = randomNino

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(
            TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true, dateOfCalculation = annualCodeDate, AnnualCode)
          )
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)
        Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }

      "an empty sequence of TaxCodeRecords is returned in TaxCodeHistory" in {
        val testNino = randomNino

        val taxCodeHistory = TaxCodeHistory(testNino.withoutSuffix, Seq.empty[TaxCodeRecord])

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)
        Await.result(service.hasTaxCodeChanged(testNino), 5.seconds) mustEqual false
      }
    }
  }

  "taxCodeChange" should {

    "return a domain TaxCodeRecord" when {

      "there has been one daily tax code change in the year after annual coding" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear
        val PreviousEndDate = currentStartDate.minusDays(1)
        val previousStartDateInPrevYear = TaxYearResolver.startOfCurrentTaxYear.minusDays(2)

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = api.TaxCodeChangeRecord("1185L", previousStartDate, PreviousEndDate, "Employer 1")
        val expectedCurrentTaxCodeChange = api.TaxCodeChangeRecord("1000L", currentStartDate, currentEndDate, "Employer 1")

        val previousTaxCodeRecord = TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = previousStartDateInPrevYear, AnnualCode)
        val currentTaxCodeRecord = TaxCodeRecord(taxCode = "1000L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = currentStartDate, NonAnnualCode)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord, currentTaxCodeRecord)
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)

        val expectedResult = TaxCodeChange(expectedCurrentTaxCodeChange, expectedPreviousTaxCodeChange)

        Await.result(service.taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been more than one daily tax code change in the year" in {
        val currentStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(2)
        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val previousStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(1)
        val PreviousEndDate = currentStartDate.minusDays(1)

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = api.TaxCodeChangeRecord("1185L", previousStartDate, PreviousEndDate, "Employer 1")
        val expectedCurrentTaxCodeChange = api.TaxCodeChangeRecord("1000L", currentStartDate, currentEndDate, "Employer 1")

        val previousTaxCodeRecord = TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = previousStartDate, NonAnnualCode)
        val currentTaxCodeRecord = TaxCodeRecord(taxCode = "1000L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = currentStartDate, NonAnnualCode)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(previousTaxCodeRecord, currentTaxCodeRecord)
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)

        val expectedResult = TaxCodeChange(expectedCurrentTaxCodeChange, expectedPreviousTaxCodeChange)

        Await.result(service.taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }

      "there has been three tax code changes in the year but the most recent tax code is not operated" in {
        val nonOperatedStartDate = TaxYearResolver.startOfCurrentTaxYear.plusDays(5)
        val currentStartDate = nonOperatedStartDate.minusDays(3)
        val previousStartDate = nonOperatedStartDate.minusDays(4)

        val currentEndDate = TaxYearResolver.endOfCurrentTaxYear
        val PreviousEndDate = currentStartDate.minusDays(1)

        val testNino = randomNino

        val expectedPreviousTaxCodeChange = api.TaxCodeChangeRecord("1185L", previousStartDate, PreviousEndDate, "Employer 1")
        val expectedCurrentTaxCodeChange = api.TaxCodeChangeRecord("1000L", currentStartDate, currentEndDate, "Employer 1")

        val previousTaxCodeRecord = TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = previousStartDate, NonAnnualCode)
        val currentTaxCodeRecord = TaxCodeRecord(taxCode = "1000L", employerName = "Employer 1", operatedTaxCode = true,
                                                  dateOfCalculation = currentStartDate, NonAnnualCode)
        val nonOperatedCode = TaxCodeRecord(taxCode = "1185L", employerName = "Employer 1", operatedTaxCode = false,
                                            dateOfCalculation = nonOperatedStartDate, NonAnnualCode)

        val taxCodeHistory = TaxCodeHistory(
          testNino.withoutSuffix,
          Seq(nonOperatedCode, previousTaxCodeRecord, currentTaxCodeRecord)
        )

        val mockConnector = mock[TaxCodeChangeConnector]
        when(mockConnector.taxCodeHistory(any(), any())).thenReturn(Future.successful(taxCodeHistory))

        val service = new TaxCodeChangeServiceImpl(mockConnector)

        val expectedResult = TaxCodeChange(expectedCurrentTaxCodeChange, expectedPreviousTaxCodeChange)

        Await.result(service.taxCodeChange(testNino), 5.seconds) mustEqual expectedResult
      }
    }
  }

  val dateFormatter = DateTimeFormat.forPattern(TaiConstants.npsDateFormat)
  def randomNino: Nino = new Generator(new Random).nextNino

}