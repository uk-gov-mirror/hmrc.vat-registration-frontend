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

package controllers.vatFinancials.vatBankAccount

import connectors.KeystoreConnector
import controllers.vatFinancials
import controllers.vatFinancials.EstimateVatTurnoverKey
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models.{CurrentProfile, S4LVatFinancials}
import models.view.vatFinancials.EstimateVatTurnover
import models.view.vatFinancials.vatBankAccount.CompanyBankAccount
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.test.FakeRequest

import scala.concurrent.Future

class CompanyBankAccountControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  object Controller extends CompanyBankAccountController(ds)(mockS4LService, mockVatRegistrationService) {
    override val authConnector = mockAuthConnector
    override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
  }

  val fakeRequest = FakeRequest(vatFinancials.vatBankAccount.routes.CompanyBankAccountController.show())

  s"GET ${vatFinancials.vatBankAccount.routes.CompanyBankAccountController.show()}" should {
    "return HTML when there's a Company Bank Account model in S4L" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      save4laterReturnsViewModel(CompanyBankAccount(CompanyBankAccount.COMPANY_BANK_ACCOUNT_YES))()

      submitAuthorised(Controller.show(),
        fakeRequest.withFormUrlEncodedBody("companyBankAccountRadio" -> "")) {
        _ includesText "Is there a bank account set up in the name of the company?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      save4laterReturnsNoViewModel[CompanyBankAccount]()
      when(mockVatRegistrationService.getVatScheme()(any(), any())).thenReturn(validVatScheme.pure)

      callAuthorised(Controller.show) {
        _ includesText "Is there a bank account set up in the name of the company?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains no data" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      save4laterReturnsNoViewModel[CompanyBankAccount]()
      when(mockVatRegistrationService.getVatScheme()(any(), any())).thenReturn(emptyVatScheme.pure)

      callAuthorised(Controller.show) {
        _ includesText "Is there a bank account set up in the name of the company?"
      }
    }
  }

  s"POST ${vatFinancials.routes.ZeroRatedSalesController.submit()}" should {
    "return 400 with Empty data" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      submitAuthorised(Controller.submit(), fakeRequest.withFormUrlEncodedBody())(_ isA 400)
    }

    "return 303 with Company Bank Account selected Yes" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      save4laterExpectsSave[CompanyBankAccount]()

      submitAuthorised(Controller.submit(),
        fakeRequest.withFormUrlEncodedBody("companyBankAccountRadio" -> CompanyBankAccount.COMPANY_BANK_ACCOUNT_YES)) {
        _ redirectsTo s"$contextRoot/business-bank-account-details"
      }
    }

    "redirect to summary if turnover is greater than 150k and Company Bank Account selected No" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      mockKeystoreFetchAndGet[Long](EstimateVatTurnoverKey.lastKnownValueKey, Some(0))
      save4laterReturnsViewModel(EstimateVatTurnover(151000L))()
      save4laterExpectsSave[CompanyBankAccount]()
      when(mockVatRegistrationService.submitVatFinancials()(any(), any())).thenReturn(validVatFinancials.pure)
      when(mockVatRegistrationService.submitVatFlatRateScheme()(any(), any())).thenReturn(validVatFlatRateScheme.pure)
      when(mockS4LService.save(any())(any(), any(), any(), any())).thenReturn(dummyCacheMap.pure)

      save4laterReturns(S4LVatFinancials())

      submitAuthorised(Controller.submit(),
        fakeRequest.withFormUrlEncodedBody("companyBankAccountRadio" -> CompanyBankAccount.COMPANY_BANK_ACCOUNT_NO)) {
        _ redirectsTo s"$contextRoot/check-your-answers"
      }
    }

    "redirect to start of FRS flow if turnover is less than 150k and Company Bank Account selected No" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      mockKeystoreFetchAndGet[Long](EstimateVatTurnoverKey.lastKnownValueKey, Some(0))
      save4laterReturnsViewModel(EstimateVatTurnover(149000L))()
      save4laterExpectsSave[CompanyBankAccount]()
      when(mockVatRegistrationService.submitVatFinancials()(any(), any())).thenReturn(validVatFinancials.pure)
      when(mockVatRegistrationService.submitVatFlatRateScheme()(any(), any())).thenReturn(validVatFlatRateScheme.pure)
      when(mockS4LService.save(any())(any(), any(), any(), any())).thenReturn(dummyCacheMap.pure)
      save4laterReturns(S4LVatFinancials())
      submitAuthorised(Controller.submit(),
        fakeRequest.withFormUrlEncodedBody("companyBankAccountRadio" -> CompanyBankAccount.COMPANY_BANK_ACCOUNT_NO)) {
        _ redirectsTo s"$contextRoot/join-flat-rate-scheme"
      }
    }

    "redirect to start of FRS flow if no turnover estimate found and Company Bank Account selected No" in {
      when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(currentProfile)))

      mockKeystoreFetchAndGet[Long](EstimateVatTurnoverKey.lastKnownValueKey, Some(0))
      save4laterReturnsNoViewModel[EstimateVatTurnover]()
      save4laterExpectsSave[CompanyBankAccount]()
      when(mockVatRegistrationService.submitVatFinancials()(any(), any())).thenReturn(validVatFinancials.pure)
      when(mockS4LService.save(any())(any(), any(), any(), any())).thenReturn(dummyCacheMap.pure)
      save4laterReturns(S4LVatFinancials())

      submitAuthorised(Controller.submit(),
        fakeRequest.withFormUrlEncodedBody("companyBankAccountRadio" -> CompanyBankAccount.COMPANY_BANK_ACCOUNT_NO)) {
        _ redirectsTo s"$contextRoot/join-flat-rate-scheme"
      }
    }
  }
}