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

package services

import java.time.LocalDate

import cats.data.OptionT
import connectors.KeystoreConnector
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models._
import models.api._
import models.external.CoHoCompanyProfile
import models.view.sicAndCompliance.BusinessActivityDescription
import models.view.sicAndCompliance.financial._
import models.view.sicAndCompliance.labour.CompanyProvideWorkers
import models.view.vatLodgingOfficer.{CompletionCapacityView, OfficerDateOfBirthView, OfficerHomeAddressView, OfficerNinoView}
import models.view.vatTradingDetails.TradingNameView
import models.view.vatTradingDetails.vatChoice.{StartDateView, VoluntaryRegistration, VoluntaryRegistrationReason}
import models._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import scala.language.postfixOps

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {

    val service = new VatRegistrationService(mockS4LService, mockRegConnector, mockCompanyRegConnector) {
      override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
    }

  }

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.createNewRegistration()(any(), any())).thenReturn(validVatScheme.pure)

      mockKeystoreCache[String]("CompanyProfile", CacheMap("", Map.empty))
      when(mockCompanyRegConnector.getCompanyRegistrationDetails(any())(any())).thenReturn(OptionT.some(validCoHoProfile))

      service.createRegistrationFootprint() completedSuccessfully
    }
  }

  "Calling createNewRegistration" should {
    "return a success response when the Registration is successfully created without finding a company profile" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.createNewRegistration()(any(), any())).thenReturn(validVatScheme.pure)
      val none: OptionT[Future, CoHoCompanyProfile] = OptionT.none
      mockKeystoreCache[String]("CompanyProfile", CacheMap("", Map.empty))
      when(mockCompanyRegConnector.getCompanyRegistrationDetails(any())(any())).thenReturn(none)

      service.createRegistrationFootprint() completedSuccessfully
    }
  }

  "Calling submitVatScheme" should {
    "return a success response when the VatScheme is upserted" in new Setup {
      mockFetchRegId(validRegId)

      save4laterReturns(validEstimateVatTurnover)
      save4laterReturns(validEstimateZeroRatedSales)
      save4laterReturns(validVatChargeExpectancy)
      save4laterReturns(validVatReturnFrequency)
      save4laterReturns(validAccountingPeriod)
      save4laterReturns(validBankAccountDetails)

      save4laterReturns(S4LVatSicAndCompliance())


      save4laterReturns(S4LVatContact())
      save4laterReturns(validServiceEligibility)

      save4laterReturns(S4LTradingDetails(
        taxableTurnover = Some(validTaxableTurnover),
        tradingName = Some(validTradingNameView),
        startDate = Some(validStartDateView),
        voluntaryRegistration = Some(VoluntaryRegistration("")),
        voluntaryRegistrationReason = Some(VoluntaryRegistrationReason("")),
        euGoods = Some(validEuGoods),
        applyEori = Some(validApplyEori)
      ))

      save4laterReturns(S4LVatLodgingOfficer())

      when(mockRegConnector.upsertVatChoice(any(), any())(any(), any())).thenReturn(validVatChoice.pure)
      when(mockRegConnector.upsertVatTradingDetails(any(), any())(any(), any())).thenReturn(validVatTradingDetails.pure)
      when(mockRegConnector.upsertVatFinancials(any(), any())(any(), any())).thenReturn(validVatFinancials.pure)
      when(mockRegConnector.upsertSicAndCompliance(any(), any())(any(), any())).thenReturn(validSicAndCompliance.pure)
      when(mockRegConnector.upsertVatContact(any(), any())(any(), any())).thenReturn(validVatContact.pure)
      when(mockRegConnector.upsertVatEligibility(any(), any())(any(), any())).thenReturn(validServiceEligibility.pure)
      when(mockRegConnector.upsertVatLodgingOfficer(any(), any())(any(), any())).thenReturn(validLodgingOfficer.pure)
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)

      service.submitVatScheme() completedSuccessfully
    }
  }

  "Calling submitTradingDetails" should {
    "return a success response when VatTradingDetails is submitted" in new Setup {
      mockFetchRegId(validRegId)
      save4laterReturns2(TradingNameView(yesNo = TradingNameView.TRADING_NAME_YES))
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertVatTradingDetails(any(), any())(any(), any())).thenReturn(validVatTradingDetails.pure)

      service.submitTradingDetails() returns validVatTradingDetails
    }

    "return a success response when start date choice is BUSINESS_START_DATE" in new Setup {

      val tradingDetailsWithCtActiveDateSelected = tradingDetails(startDateSelection = StartDateView.BUSINESS_START_DATE)

      mockFetchRegId(validRegId)
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertVatTradingDetails(any(), any())(any(), any())).thenReturn(tradingDetailsWithCtActiveDateSelected.pure)
      save4laterReturns2(StartDateView(StartDateView.BUSINESS_START_DATE, someTestDate))

      service.submitTradingDetails() returns tradingDetailsWithCtActiveDateSelected
    }

    "return a success response when VatTradingDetails is submitted and no Trading Name is found in S4L" in new Setup {
      mockFetchRegId(validRegId)
      save4laterReturnsNothing2[TradingNameView]()
      save4laterReturns2(validEuGoods)
      save4laterReturns2(validApplyEori)
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertVatTradingDetails(any(), any())(any(), any())).thenReturn(validVatTradingDetails.pure)

      service.submitTradingDetails() returns validVatTradingDetails
    }
  }

  "Calling submitSicAndCompliance" should {
    "return a success response when SicAndCompliance is submitted" in new Setup {
      mockFetchRegId(validRegId)

      save4laterReturns(validBusinessActivityDescription)
      save4laterReturns(validNotForProfit)
      save4laterReturns(validCompanyProvideWorkers)
      save4laterReturns(validWorkers)
      save4laterReturns(validTemporaryContracts)
      save4laterReturns(validAdviceOrConsultancy)
      save4laterReturns(validActAsIntermediary)
      save4laterReturns(ChargeFees(true))
      save4laterReturns(AdditionalNonSecuritiesWork(true))
      save4laterReturns(DiscretionaryInvestmentManagementServices(true))
      save4laterReturns(InvestmentFundManagement(true))
      save4laterReturns(ManageAdditionalFunds(true))

      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertSicAndCompliance(any(), any())(any(), any())).thenReturn(validSicAndCompliance.pure)

      service.submitSicAndCompliance() returns validSicAndCompliance
    }

    "return a success response when SicAndCompliance is submitted and no Business Activity Description is found in S4L" in new Setup {
      mockFetchRegId(validRegId)

      save4laterReturnsNothing[BusinessActivityDescription]()
      save4laterReturnsNothing[CompanyProvideWorkers]()
      save4laterReturns(validSkilledWorkers)

      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertSicAndCompliance(any(), any())(any(), any())).thenReturn(validSicAndCompliance.pure)

      service.submitSicAndCompliance() returns validSicAndCompliance
    }
  }

  "Calling submitEligibility" should {
    "return a success response when VatEligibility is submitted" in new Setup {
      mockFetchRegId(validRegId)

      save4laterReturns(validServiceEligibility)

      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(validVatScheme.pure)
      when(mockRegConnector.upsertVatEligibility(any(), any())(any(), any())).thenReturn(validServiceEligibility.pure)

      service.submitVatEligibility() returns validServiceEligibility
    }
  }

  "Calling deleteVatScheme" should {
    "return a success response when the delete VatScheme is successful" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.deleteVatScheme(any())(any(), any())).thenReturn(().pure)

      service.deleteVatScheme() completedSuccessfully
    }
  }

  "Calling deleteElement" should {
    "return a success response when successful" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.deleteElement(any())(any())(any(), any())).thenReturn(().pure)

      service.deleteElement(VatBankAccountPath) completedSuccessfully
    }
  }

  "Calling deleteElements with items" should {
    "return a success response when successful" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.deleteElement(any())(any())(any(), any())).thenReturn(().pure)

      service.deleteElements(List(VatBankAccountPath, ZeroRatedTurnoverEstimatePath)) completedSuccessfully
    }
  }

  "Calling deleteElements without items" should {
    "return a success response when successful" in new Setup {
      mockKeystoreCache[String]("RegistrationId", CacheMap("", Map.empty))
      when(mockRegConnector.deleteElement(any())(any())(any(), any())).thenReturn(().pure)

      service.deleteElements(List()) completedSuccessfully
    }
  }

  "When this is the first time the user starts a journey and we're persisting to the backend" should {

    "submitVatFinancials should process the submission even if VatScheme does not contain a VatFinancials object" in new Setup {
      val mergedVatFinancials = VatFinancials(
        bankAccount = Some(validBankAccount),
        turnoverEstimate = 50000,
        zeroRatedTurnoverEstimate = Some(60000),
        reclaimVatOnMostReturns = true,
        accountingPeriods = monthlyAccountingPeriod
      )

      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)

      service.submitVatFinancials() returns mergedVatFinancials
    }

    "submitTradingDetails should process the submission even if VatScheme does not contain a VatFinancials object" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      service.submitTradingDetails() returns validVatTradingDetails
    }

    "submitTradingDetails should fail if there's not trace of VatTradingDetails in neither backend nor S4L" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      save4laterReturnsNothing[S4LTradingDetails]()

      service.submitTradingDetails() failedWith classOf[IllegalStateException]
    }

    "submitVatContact should process the submission even if VatScheme does not contain a VatContact object" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      service.submitVatContact() returns validVatContact
    }

    "submitVatContact should fail if there's not trace of VatContact in neither backend nor S4L" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      save4laterReturnsNothing[S4LVatContact]()

      service.submitVatContact() failedWith classOf[IllegalStateException]
    }

    "submitVatEligibility should process the submission even if VatScheme does not contain a VatEligibility object" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      service.submitVatEligibility() returns validServiceEligibility
    }

    "submitVatLodgingOfficer should process the submission even if VatScheme does not contain a VatLodgingOfficer object" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      service.submitVatLodgingOfficer() returns validLodgingOfficer
    }

    "submitVatLodgingOfficer should fail if there's not trace of VatLodgingOfficer in neither backend nor S4L" in new Setup {
      when(mockRegConnector.getRegistration(Matchers.eq(validRegId))(any(), any())).thenReturn(emptyVatScheme.pure)
      save4laterReturnsNothing[S4LVatLodgingOfficer]()

      service.submitVatLodgingOfficer() failedWith classOf[IllegalStateException]
    }

  }
}
