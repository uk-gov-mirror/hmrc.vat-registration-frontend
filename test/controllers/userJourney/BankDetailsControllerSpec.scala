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

package controllers.userJourney

import builders.AuthBuilder
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.CacheKey
import models.view.CompanyBankAccountDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatRegistrationService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class BankDetailsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  val mockVatRegistrationService = mock[VatRegistrationService]

  object BankDetailsController extends BankDetailsController(ds)(mockS4LService, mockVatRegistrationService) {
    override val authConnector = mockAuthConnector
  }

  val fakeRequest = FakeRequest(routes.BankDetailsController.show())
  val validBankAccountFormData = Seq(
    "accountName" -> "Some account name",
    "accountNumber" -> "12345678",
    "sortCode.part1" -> "11",
    "sortCode.part2" -> "22",
    "sortCode.part3" -> "33"
  )

  s"GET ${routes.BankDetailsController.show()}" should {

    "return HTML when there's a CompanyBankAccountDetails model in S4L" in {
      val companyBankAccountDetails = CompanyBankAccountDetails()

      when(mockS4LService.fetchAndGet[CompanyBankAccountDetails]()
        (Matchers.eq(CacheKey[CompanyBankAccountDetails]), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(companyBankAccountDetails)))

      callAuthorised(BankDetailsController.show(), mockAuthConnector) {
        result =>
          status(result) mustBe OK
          contentType(result) mustBe Some("text/html")
          charset(result) mustBe Some("utf-8")
          contentAsString(result) must include("What are your business bank account details?")
      }
    }

    "return HTML when there's nothing in S4L" in {
      when(mockS4LService.fetchAndGet[CompanyBankAccountDetails]()
        (Matchers.eq(CacheKey[CompanyBankAccountDetails]), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      when(mockVatRegistrationService.getVatScheme()(Matchers.any()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(BankDetailsController.show, mockAuthConnector) {
        result =>
          status(result) mustBe OK
          contentType(result) mustBe Some("text/html")
          charset(result) mustBe Some("utf-8")
          contentAsString(result) must include("What are your business bank account details?")
      }
    }
  }


  s"POST ${routes.ZeroRatedSalesController.submit()} with Empty data" should {

    "return 400" in {
      AuthBuilder.submitWithAuthorisedUser(BankDetailsController.submit(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
      )) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }

    }
  }


  s"POST ${routes.BankDetailsController.submit()} with valid Company Bank Account Details" should {

    "return 303" in {
      val returnCacheMapCompanyBankAccount = CacheMap("", Map("" -> Json.toJson(CompanyBankAccountDetails())))

      when(mockS4LService.saveForm[CompanyBankAccountDetails]
        (Matchers.any())(Matchers.eq(CacheKey[CompanyBankAccountDetails]), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnCacheMapCompanyBankAccount))

      AuthBuilder.submitWithAuthorisedUser(BankDetailsController.submit(), mockAuthConnector,
        fakeRequest.withFormUrlEncodedBody(validBankAccountFormData: _*)) {
        response =>
          status(response) mustBe Status.SEE_OTHER
          redirectLocation(response).getOrElse("") mustBe "/vat-registration/estimate-vat-turnover"
      }
    }
  }

  s"POST ${routes.BankDetailsController.submit()} with invalid Company Bank Account Details" should {

    "return 400" in {
      val returnCacheMapCompanyBankAccount = CacheMap("", Map("" -> Json.toJson(CompanyBankAccountDetails())))

      when(mockS4LService.saveForm[CompanyBankAccountDetails]
        (Matchers.any())(Matchers.eq(CacheKey[CompanyBankAccountDetails]), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnCacheMapCompanyBankAccount))

      val invalidBankAccountFormData = validBankAccountFormData.drop(1)

      AuthBuilder.submitWithAuthorisedUser(BankDetailsController.submit(), mockAuthConnector,
        fakeRequest.withFormUrlEncodedBody(invalidBankAccountFormData: _*)) {
        response =>
          status(response) mustBe Status.BAD_REQUEST
      }
    }
  }

}
