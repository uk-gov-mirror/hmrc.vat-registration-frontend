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

import javax.inject.Inject

import cats.data.OptionT
import com.google.inject.ImplementedBy
import connectors.{CompanyRegistrationConnector, VatRegistrationConnector}
import models._
import models.api._
import models.external.CoHoCompanyProfile
import models.view.sicAndCompliance.BusinessActivityDescription
import models.view.sicAndCompliance.cultural.NotForProfit
import models.view.sicAndCompliance.financial._
import models.view.sicAndCompliance.labour.{CompanyProvideWorkers, SkilledWorkers, TemporaryContracts, Workers}
import models.view.vatContact.BusinessContactDetails
import models.view.vatFinancials._
import models.view.vatFinancials.vatAccountingPeriod.{AccountingPeriod, VatReturnFrequency}
import models.view.vatFinancials.vatBankAccount.CompanyBankAccountDetails
import models.view.vatLodgingOfficer.OfficerHomeAddressView
import models.view.vatTradingDetails.TradingNameView
import models.view.vatTradingDetails.vatChoice.{StartDateView, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.vatTradingDetails.vatEuTrading.{ApplyEori, EuGoods}
import play.api.libs.json.Format
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[VatRegistrationService])
trait RegistrationService {

  def createRegistrationFootprint()(implicit hc: HeaderCarrier): Future[Unit]

  def submitVatScheme()(implicit hc: HeaderCarrier): Future[Unit]

  def getVatScheme()(implicit hc: HeaderCarrier): Future[VatScheme]

  def deleteElement(elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Unit]

  def deleteElements(elementPath: List[ElementPath])(implicit hc: HeaderCarrier): Future[Unit]

}

class VatRegistrationService @Inject()(s4LService: S4LService,
                                       vatRegConnector: VatRegistrationConnector,
                                       companyRegistrationConnector: CompanyRegistrationConnector)

  extends RegistrationService
    with CommonService {

  import cats.instances.future._
  import cats.syntax.cartesian._
  import cats.instances.list._
  import cats.syntax.foldable._

  private def s4l[T: Format : S4LKey]()(implicit headerCarrier: HeaderCarrier) = s4LService.fetchAndGet[T]()

  private def update[C, G](c: Option[C], vs: VatScheme)(implicit vmTransformer: ViewModelTransformer[C, G]): G => G =
    g => c.map(vmTransformer.toApi(_, g)).getOrElse(g)

  def getVatScheme()(implicit hc: HeaderCarrier): Future[VatScheme] =
    fetchRegistrationId.flatMap(vatRegConnector.getRegistration)

  def deleteVatScheme()(implicit hc: HeaderCarrier): Future[Unit] =
    fetchRegistrationId.flatMap(vatRegConnector.deleteVatScheme)

  def deleteElement(elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Unit] =
    fetchRegistrationId.flatMap(vatRegConnector.deleteElement(elementPath))

  def deleteElements(elementPaths: List[ElementPath])(implicit hc: HeaderCarrier): Future[Unit] =
    elementPaths.traverse_(deleteElement)


  def createRegistrationFootprint()(implicit hc: HeaderCarrier): Future[Unit] =
    (for {
      vatScheme <- OptionT.liftF(vatRegConnector.createNewRegistration())
      _ <- OptionT.liftF(keystoreConnector.cache[String]("RegistrationId", vatScheme.id))
      companyProfile <- companyRegistrationConnector.getCompanyRegistrationDetails(vatScheme.id)
      _ <- OptionT.liftF(keystoreConnector.cache[CoHoCompanyProfile]("CompanyProfile", companyProfile))
    } yield ()).getOrElse(())

  def submitVatScheme()(implicit hc: HeaderCarrier): Future[Unit] =
    submitTradingDetails |@| submitVatFinancials |@| submitSicAndCompliance |@|
      submitVatContact |@| submitVatEligibility() |@| submitVatLodgingOfficer map { case _ => () }

  private[services] def submitVatFinancials()(implicit hc: HeaderCarrier): Future[VatFinancials] = {

    def mergeWithS4L(vs: VatScheme) =
      (s4l[EstimateVatTurnover]() |@|
        s4l[EstimateZeroRatedSales]() |@|
        s4l[VatChargeExpectancy]() |@|
        s4l[VatReturnFrequency]() |@|
        s4l[AccountingPeriod]() |@|
        s4l[CompanyBankAccountDetails]())
        .map(S4LVatFinancials).map { s4l =>
        update(s4l.estimateVatTurnover, vs)
          .andThen(update(s4l.zeroRatedTurnoverEstimate, vs))
          .andThen(update(s4l.vatChargeExpectancy, vs))
          .andThen(update(s4l.vatReturnFrequency, vs))
          .andThen(update(s4l.accountingPeriod, vs))
          .andThen(update(s4l.companyBankAccountDetails, vs))
          .apply(vs.financials.getOrElse(VatFinancials.empty)) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      vatFinancials <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertVatFinancials(vs.id, vatFinancials)
    } yield response
  }

  private[services] def submitSicAndCompliance()(implicit hc: HeaderCarrier): Future[VatSicAndCompliance] = {
    def mergeWithS4L(vs: VatScheme) =
      (s4l[BusinessActivityDescription]() |@|
        s4l[NotForProfit]() |@|
        s4l[CompanyProvideWorkers]() |@|
        s4l[Workers]() |@|
        s4l[TemporaryContracts]() |@|
        s4l[SkilledWorkers]() |@|
        s4l[AdviceOrConsultancy]() |@|
        s4l[ActAsIntermediary]() |@|
        s4l[ChargeFees]() |@|
        s4l[LeaseVehicles]() |@|
        s4l[AdditionalNonSecuritiesWork]() |@|
        s4l[DiscretionaryInvestmentManagementServices]() |@|
        s4l[InvestmentFundManagement]() |@|
        s4l[ManageAdditionalFunds]())
        .map(S4LVatSicAndCompliance).map { s4l =>
        update(s4l.description, vs)
          .andThen(update(s4l.notForProfit, vs))
          .andThen(update(s4l.companyProvideWorkers, vs))
          .andThen(update(s4l.workers, vs))
          .andThen(update(s4l.temporaryContracts, vs))
          .andThen(update(s4l.skilledWorkers, vs))
          .andThen(update(s4l.adviceOrConsultancy, vs))
          .andThen(update(s4l.actAsIntermediary, vs))
          .andThen(update(s4l.chargeFees, vs))
          .andThen(update(s4l.leaseVehicles, vs))
          .andThen(update(s4l.additionalNonSecuritiesWork, vs))
          .andThen(update(s4l.discretionaryInvestmentManagementServices, vs))
          .andThen(update(s4l.investmentFundManagement, vs))
          .andThen(update(s4l.manageAdditionalFunds, vs))
          .apply(vs.vatSicAndCompliance.getOrElse(VatSicAndCompliance(""))) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      sicAndCompliance <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertSicAndCompliance(vs.id, sicAndCompliance)
    } yield response
  }

  private[services] def submitTradingDetails()(implicit hc: HeaderCarrier): Future[VatTradingDetails] = {
    def mergeWithS4L(vs: VatScheme) =
      (s4l[TradingNameView]() |@|
        s4l[StartDateView]() |@|
        s4l[VoluntaryRegistration]() |@|
        s4l[VoluntaryRegistrationReason]() |@|
        s4l[EuGoods]() |@|
        s4l[ApplyEori]())
        .map(S4LTradingDetails).map { s4l =>
        update(s4l.voluntaryRegistration, vs)
          .andThen(update(s4l.tradingName, vs))
          .andThen(update(s4l.startDate, vs))
          .andThen(update(s4l.voluntaryRegistrationReason, vs))
          .andThen(update(s4l.euGoods, vs))
          .andThen(update(s4l.applyEori, vs))
          .apply(vs.tradingDetails.getOrElse(VatTradingDetails.empty)) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      vatTradingDetails <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertVatTradingDetails(vs.id, vatTradingDetails)
    } yield response
  }


  private[services] def submitVatContact()(implicit hc: HeaderCarrier): Future[VatContact] = {
    def mergeWithS4L(vs: VatScheme) =
      s4l[BusinessContactDetails]().map(S4LVatContact).map { s4l =>
        update(s4l.businessContactDetails, vs)
          .apply(vs.vatContact.getOrElse(VatContact.empty)) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      vatContact <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertVatContact(vs.id, vatContact)
    } yield response
  }

  private[services] def submitVatEligibility()(implicit hc: HeaderCarrier): Future[VatServiceEligibility] = {
    def mergeWithS4L(vs: VatScheme) =
      s4l[VatServiceEligibility]().map(S4LVatEligibility).map { s4l =>
        update(s4l.vatEligibility, vs)
          .apply(vs.vatServiceEligibility.getOrElse(VatServiceEligibility())) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      vatServiceEligibility <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertVatEligibility(vs.id, vatServiceEligibility)
    } yield response
  }

  private[services] def submitVatLodgingOfficer()(implicit hc: HeaderCarrier): Future[VatLodgingOfficer] = {
    def mergeWithS4L(vs: VatScheme) =
      s4l[OfficerHomeAddressView]().map(S4LVatLodgingOfficer).map { s4l =>
        update(s4l.officerHomeAddressView, vs)
          .apply(vs.lodgingOfficer.getOrElse(VatLodgingOfficer.empty)) //TODO remove the "seeding" with empty
      }

    for {
      vs <- getVatScheme()
      vatLodgingOfficer <- mergeWithS4L(vs)
      response <- vatRegConnector.upsertVatLodgingOfficer(vs.id, vatLodgingOfficer)
    } yield response
  }

}
