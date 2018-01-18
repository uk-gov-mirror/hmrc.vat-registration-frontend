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

package controllers.builders

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.view.SummaryRow

class SummaryDoingBusinessAbroadSectionBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  "The section builder composing a company details section" should {

    val bankAccount = VatBankAccount(accountNumber = "12345678", accountName = "Account Name", accountSortCode = testSortCode)

    "with EU Goods render" should {

      "a false value should be returned with a row displaying 'No'" in {
        val details = generateTradingDetails(euGoodsSelection = false)
        val builder = SummaryDoingBusinessAbroadSectionBuilder(tradingDetails = Some(details))
        builder.euGoodsRow mustBe
          SummaryRow(
            "doingBusinessAbroad.eori.euGoods",
            "app.common.no",
            Some(controllers.routes.TradingDetailsController.euGoodsPage())
          )
      }

      "a true value should be returned with a row displaying 'Yes'" in {
        val details =  generateTradingDetails()
        val builder = SummaryDoingBusinessAbroadSectionBuilder(tradingDetails = Some(details))
        builder.euGoodsRow mustBe
          SummaryRow(
            "doingBusinessAbroad.eori.euGoods",
            "app.common.yes",
            Some(controllers.routes.TradingDetailsController.euGoodsPage())
          )
      }
    }

    "with apply EORI render" should {

      "a 'No' value should be returned with a 'No' " in {
        val builder = SummaryDoingBusinessAbroadSectionBuilder()
        builder.applyEoriRow mustBe
          SummaryRow(
            "doingBusinessAbroad.eori",
            "app.common.no",
            Some(controllers.routes.TradingDetailsController.applyEoriPage())
          )
      }

      "a 'Yes' value should be returned with a 'Yes' " in {
        val details =  generateTradingDetails( eoriApplication = Some(true))
        val builder = SummaryDoingBusinessAbroadSectionBuilder(tradingDetails = Some(details))
        builder.applyEoriRow mustBe
          SummaryRow(
            "doingBusinessAbroad.eori",
            "app.common.yes",
            Some(controllers.routes.TradingDetailsController.applyEoriPage())
          )
      }
    }

    "with section generate" should {

      "a valid summary section" in {
        val tradingDetails = generateTradingDetails()
        val builder = SummaryDoingBusinessAbroadSectionBuilder(tradingDetails = Some(tradingDetails))
        builder.section.id mustBe "doingBusinessAbroad"
        builder.section.rows.length mustEqual 2
      }
    }

  }
}
