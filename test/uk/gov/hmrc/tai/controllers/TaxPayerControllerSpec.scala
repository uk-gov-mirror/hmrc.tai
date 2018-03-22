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

package uk.gov.hmrc.tai.controllers

import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Generator
import play.api.test.Helpers._

import scala.util.Random

class TaxPayerControllerSpec extends PlaySpec{

  "taxPayer" should {
    "return 200" when{
      "given a valid nino" in{
        val sut = new TaxPayerController{}
        val result = sut.taxPayer(new Generator(new Random).nextNino)(FakeRequest())
        status(result) mustBe OK
      }
    }
  }

}
