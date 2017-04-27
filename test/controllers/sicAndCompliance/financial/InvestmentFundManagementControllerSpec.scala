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

package controllers.sicAndCompliance.financial

import builders.AuthBuilder
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.S4LKey
import models.view.sicAndCompliance.financial.InvestmentFundManagement
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.VatRegistrationService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class InvestmentFundManagementControllerSpec extends VatRegSpec with VatRegistrationFixture {

  val mockVatRegistrationService = mock[VatRegistrationService]

  object InvestmentFundManagementController extends InvestmentFundManagementController(ds)(mockS4LService, mockVatRegistrationService) {
    override val authConnector = mockAuthConnector
  }

  val fakeRequest = FakeRequest(routes.InvestmentFundManagementController.show())

  s"GET ${routes.InvestmentFundManagementController.show()}" should {

    "return HTML when there's a Investment Fund Management model in S4L" in {
      val chargeFees = InvestmentFundManagement(true)

      when(mockS4LService.fetchAndGet[InvestmentFundManagement]()(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(Some(chargeFees)))

      AuthBuilder.submitWithAuthorisedUser(InvestmentFundManagementController.show(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "investmentFundManagementRadio" -> ""
      )) {
        _ includesText "Does the company provide investment fund management services?"
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in {
      when(mockS4LService.fetchAndGet[InvestmentFundManagement]()
        (Matchers.eq(S4LKey[InvestmentFundManagement]), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(None))

      when(mockVatRegistrationService.getVatScheme()(Matchers.any()))
        .thenReturn(Future.successful(validVatScheme))

      callAuthorised(InvestmentFundManagementController.show) {
       _ includesText "Does the company provide investment fund management services?"
      }
    }
  }

  "return HTML when there's nothing in S4L and vatScheme contains no data" in {
    when(mockS4LService.fetchAndGet[InvestmentFundManagement]()
      (Matchers.eq(S4LKey[InvestmentFundManagement]), Matchers.any(), Matchers.any()))
      .thenReturn(Future.successful(None))

    when(mockVatRegistrationService.getVatScheme()(Matchers.any[HeaderCarrier]()))
      .thenReturn(Future.successful(emptyVatScheme))

    callAuthorised(InvestmentFundManagementController.show) {
     _ includesText "Does the company provide investment fund management services?"
    }
  }

  s"POST ${routes.InvestmentFundManagementController.show()} with Empty data" should {

    "return 400" in {
      AuthBuilder.submitWithAuthorisedUser(InvestmentFundManagementController.submit(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
      )) {
        result =>
          status(result) mustBe Status.BAD_REQUEST
      }

    }
  }

  s"POST ${routes.InvestmentFundManagementController.submit()} with Investment Fund Management Yes selected" should {

    "return 303" in {
      val returnCacheMapInvestmentFundManagement = CacheMap("", Map("" -> Json.toJson(InvestmentFundManagement(true))))

      when(mockVatRegistrationService.deleteElements(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(true))

      when(mockS4LService.saveForm[InvestmentFundManagement]
        (Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnCacheMapInvestmentFundManagement))

      AuthBuilder.submitWithAuthorisedUser(InvestmentFundManagementController.submit(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "investmentFundManagementRadio" -> "true"
      )) {
        response =>
          response redirectsTo s"$contextRoot/manages-funds-not-included-in-this-list"
      }

    }
  }

  s"POST ${routes.InvestmentFundManagementController.submit()} with Investment Fund Management No selected" should {

    "return 303" in {
      val returnCacheMapInvestmentFundManagement = CacheMap("", Map("" -> Json.toJson(InvestmentFundManagement(false))))

      when(mockVatRegistrationService.deleteElements(Matchers.any())(Matchers.any()))
        .thenReturn(Future.successful(true))

      when(mockS4LService.saveForm[InvestmentFundManagement]
        (Matchers.any())(Matchers.any(), Matchers.any(), Matchers.any()))
        .thenReturn(Future.successful(returnCacheMapInvestmentFundManagement))

      AuthBuilder.submitWithAuthorisedUser(InvestmentFundManagementController.submit(), mockAuthConnector, fakeRequest.withFormUrlEncodedBody(
        "investmentFundManagementRadio" -> "false"
      )) {
        response =>
          response redirectsTo s"$contextRoot/company-bank-account"
      }

    }
  }
}