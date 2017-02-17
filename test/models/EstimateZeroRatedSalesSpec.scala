/*
 * Copyright 2017 HM Revenue & Customs
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

import fixtures.VatRegistrationFixture
import models.api.VatFinancials
import models.view.EstimateZeroRatedSales
import uk.gov.hmrc.play.test.UnitSpec

class EstimateZeroRatedSalesSpec extends UnitSpec with VatRegistrationFixture {

  override val validEstimateZeroRatedSales = EstimateZeroRatedSales(Some(60000L))
  override val differentEstimateZeroRatedSales = EstimateZeroRatedSales(Some(20000L))

  val validVatFinancials = VatFinancials(
    turnoverEstimate = 50000L, zeroRatedSalesEstimate = 60000L
  )

  val differentVatFinancials = VatFinancials(
    turnoverEstimate = 50000L, zeroRatedSalesEstimate = 20000L
  )

  "toApi" should {
    "upserts (merge) a current VatFinancials API model with the details of an instance of EstimateZeroRatedSales view model" in {
      differentEstimateZeroRatedSales.toApi(validVatFinancials) shouldBe differentVatFinancials
    }
  }

  "apply" should {
    "convert a populated VatScheme's VatFinancials API model to an instance of EstimateZeroRatedSales view model" in {
      EstimateZeroRatedSales.apply(validVatScheme) shouldBe EstimateZeroRatedSales(None)
    }
  }
}
