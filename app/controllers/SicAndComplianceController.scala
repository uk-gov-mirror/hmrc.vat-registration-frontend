/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import config.{AuthClientConnector, BaseControllerComponents, FrontendAppConfig}
import connectors.KeystoreConnector
import featureswitch.core.config.{FeatureSwitching, StubIcl}
import forms.MainBusinessActivityForm
import javax.inject.{Inject, Singleton}
import models.ModelKeys.SIC_CODES_KEY
import models.api.SicCode
import models.{CurrentProfile, MainBusinessActivityView}
import play.api.i18n.Messages
import play.api.mvc.{Action, AnyContent, Result}
import services._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SicAndComplianceController @Inject()(val authConnector: AuthClientConnector,
                                           val keystoreConnector: KeystoreConnector,
                                           val sicAndCompService: SicAndComplianceService,
                                           val frsService: FlatRateService,
                                           val iclService: ICLService
                                          )(implicit appConfig: FrontendAppConfig,
                                            val executionContext: ExecutionContext,
                                            baseControllerComponents: BaseControllerComponents) extends BaseController with SessionProfile with FeatureSwitching {

  val iclFEurlwww: String = appConfig.servicesConfig.getConfString("industry-classification-lookup-frontend.www.url",
    throw new RuntimeException("[ICLConnector] Could not retrieve config for 'industry-classification-lookup-frontend.www.url'"))

  private def fetchSicCodeList()(implicit hc: HeaderCarrier): Future[List[SicCode]] =
    keystoreConnector.fetchAndGet[List[SicCode]](SIC_CODES_KEY) map (_.getOrElse(List.empty[SicCode]))

  def showMainBusinessActivity: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        for {
          sicCodeList <- fetchSicCodeList
          sicCompliance <- sicAndCompService.getSicAndCompliance
          formFilled = sicCompliance.mainBusinessActivity.fold(MainBusinessActivityForm.form)(MainBusinessActivityForm.form.fill)
        } yield Ok(main_business_activity(formFilled, sicCodeList))
  }

  def submitMainBusinessActivity: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        fetchSicCodeList flatMap { sicCodeList =>
          MainBusinessActivityForm.form.bindFromRequest().fold(
            badForm => Future.successful(BadRequest(main_business_activity(badForm, sicCodeList))),
            data => sicCodeList.find(_.code == data.id).fold(
              Future.successful(BadRequest(main_business_activity(MainBusinessActivityForm.form.fill(data), sicCodeList)))
            )(selected => for {
              _ <- sicAndCompService.updateSicAndCompliance(MainBusinessActivityView(selected))
              _ <- frsService.resetFRSForSAC(selected)
            } yield {
              if (sicAndCompService.needComplianceQuestions(sicCodeList)) {
                Redirect(routes.SicAndComplianceController.showComplianceIntro())
              } else {
                Redirect(controllers.registration.business.routes.TradingNameController.show())
              }
            })
          )
        }
  }

  def showComplianceIntro: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request => _ =>
      Future.successful(Ok(compliance_introduction()))
  }

  def submitComplianceIntro: Action[AnyContent] = isAuthenticatedWithProfile() {
    _ => _ => Future.successful(Redirect(controllers.registration.sicandcompliance.routes.SupplyWorkersController.show()))
  }

  def showSicHalt: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request => _ =>
      Future.successful(Ok(about_to_confirm_sic()))
  }

  private def startSelectingNewSicCodes(implicit hc: HeaderCarrier, cp: CurrentProfile, messages: Messages): Future[Result] = {
    if (isEnabled(StubIcl)) {
      Future.successful(Redirect(controllers.test.routes.SicStubController.show()))
    } else {
      val customICLMessages: CustomICLMessages = CustomICLMessages(
        messages("pages.icl.heading"),
        messages("pages.icl.lead"),
        messages("pages.icl.hint")
      )

      iclService.journeySetup(customICLMessages) map (redirectUrl => Redirect(iclFEurlwww + redirectUrl, 303))
    }
  }

  def submitSicHalt: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        startSelectingNewSicCodes
  }

  def returnToICL: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        startSelectingNewSicCodes
  }

  def saveIclCodes: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        for {
          iclCodes <- iclService.getICLSICCodes()
          _ <- keystoreConnector.cache(SIC_CODES_KEY, iclCodes)
          _ <- sicAndCompService.submitSicCodes(iclCodes)
        } yield {
          Redirect(iclCodes match {
            case codes if codes.size > 1 =>
              routes.SicAndComplianceController.showMainBusinessActivity()
            case codes if sicAndCompService.needComplianceQuestions(codes) =>
              controllers.routes.SicAndComplianceController.showComplianceIntro()
            case List(onlyOneCode) =>
              controllers.registration.business.routes.TradingNameController.show()
            case List() =>
              throw new InternalServerException("[SicAndComplianceController][saveIclCodes] tried to save empty list")
          })
        }
  }
}
