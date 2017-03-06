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

package models.view

import fixtures.VatRegistrationFixture
import models.ApiModelTransformer
import models.api.{VatAccountingPeriod, VatBankAccount, VatFinancials, VatScheme}
import models.view.CompanyBankAccount.{COMPANY_BANK_ACCOUNT_NO, COMPANY_BANK_ACCOUNT_YES}
import uk.gov.hmrc.play.test.UnitSpec

class CompanyBankAccountSpec extends UnitSpec with VatRegistrationFixture {

  "apply" should {
    val vatScheme = VatScheme(validRegId)
    val someBankAccount = VatBankAccount(accountName = "test", accountNumber = "12345678", accountSortCode = "12-12-12")

    "convert VatFinancials with bank account to view model" in {
      val vatFinancialsWithAccount = VatFinancials(
        bankAccount = Some(someBankAccount),
        turnoverEstimate = 100L,
        reclaimVatOnMostReturns = true,
        vatAccountingPeriod = VatAccountingPeriod(None, "monthly")
      )
      val vs = vatScheme.copy(financials = Some(vatFinancialsWithAccount))
      ApiModelTransformer[CompanyBankAccount].toViewModel(vs) shouldBe Some(CompanyBankAccount(COMPANY_BANK_ACCOUNT_YES))
    }

    "convert VatFinancials without bank account to view model" in {
      val vatFinancialsWithoutAccount = VatFinancials(
        turnoverEstimate = 100L,
        reclaimVatOnMostReturns = true,
        vatAccountingPeriod = VatAccountingPeriod(None, "monthly")
      )
      val vs = vatScheme.copy(financials = Some(vatFinancialsWithoutAccount))

      ApiModelTransformer[CompanyBankAccount].toViewModel(vs) shouldBe Some(CompanyBankAccount(COMPANY_BANK_ACCOUNT_NO))
    }

    "convert VatScheme without VatFinancials to empty view model" in {
      val vs = vatScheme.copy(financials = None)
      ApiModelTransformer[CompanyBankAccount].toViewModel(vs) shouldBe None
    }
  }
}

