/*
 * Copyright 2020 HM Revenue & Customs
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

import config.AuthClientConnector
import connectors.KeystoreConnector
import forms._
import models.DateSelection
import views.html.vatAccountingPeriod.{accounting_period_view => AccountingPeriodPage, return_frequency_view => ReturnFrequencyPage}
import views.html.{charge_expectancy_view => ChargeExpectancyPage, mandatory_start_date_confirmation => MandatoryStartDateConfirmationPage, mandatory_start_date_incorp_view => MandatoryStartDateIncorpPage, start_date_incorp_view => VoluntaryStartDateIncorpPage, start_date_view => VoluntaryStartDatePage}
import forms.{AccountingPeriodForm, ChargeExpectancyForm, MandatoryDateForm, ReturnFrequencyForm, VoluntaryDateForm, VoluntaryDateFormIncorp}
import javax.inject.Inject
import models.{CurrentProfile, DateSelection, Frequency, MonthYearModel, Returns}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.{ReturnsService, SessionProfile, TimeService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.workingdays.BankHolidaySet

import scala.concurrent.Future
import scala.language.postfixOps

class ReturnsControllerImpl @Inject()(val keystoreConnector: KeystoreConnector,
                                      val authConnector: AuthClientConnector,
                                      val returnsService: ReturnsService,
                                      val messagesApi: MessagesApi,
                                      val timeService: TimeService) extends ReturnsController

trait ReturnsController extends BaseController with SessionProfile {

  val returnsService: ReturnsService
  val authConnector: AuthConnector
  val keystoreConnector: KeystoreConnector
  val timeService: TimeService

  val chargeExpectancyPage: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          returnsService.getReturns map { returns =>
            returns.reclaimVatOnMostReturns match {
              case Some(chargeExpectancy) => Ok(ChargeExpectancyPage(ChargeExpectancyForm.form.fill(chargeExpectancy)))
              case None => Ok(ChargeExpectancyPage(ChargeExpectancyForm.form))
            }
          }
        }
  }

  val submitChargeExpectancy: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          ChargeExpectancyForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(ChargeExpectancyPage(errors))),
            success => {
              returnsService.saveReclaimVATOnMostReturns(success) flatMap { _ =>
                correctVatStartDatePage()
              }
            }
          )
        }
  }

  val accountPeriodsPage: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          returnsService.getReturns map { returns =>
            returns.staggerStart match {
              case Some(stagger) => Ok(AccountingPeriodPage(AccountingPeriodForm.form.fill(stagger)))
              case None => Ok(AccountingPeriodPage(AccountingPeriodForm.form))
            }
          }
        }
  }

  private def correctVatStartDatePage()(implicit hc: HeaderCarrier, currentProfile: CurrentProfile): Future[Result] =
    returnsService.getThreshold map { voluntary =>
      if (voluntary) Redirect(routes.ReturnsController.voluntaryStartPage()) else Redirect(routes.ReturnsController.mandatoryStartPage())
    }

  val submitAccountPeriods: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          AccountingPeriodForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(AccountingPeriodPage(errors))),
            success => returnsService.saveStaggerStart(success) map { _ =>
              Redirect(controllers.routes.BankAccountDetailsController.showHasCompanyBankAccountView())
            }
          )
        }
  }

  val returnsFrequencyPage: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          returnsService.getReturns map { returns =>
            returns.frequency match {
              case Some(frequency) => Ok(ReturnFrequencyPage(ReturnFrequencyForm.form.fill(frequency)))
              case None => Ok(ReturnFrequencyPage(ReturnFrequencyForm.form))
            }
          }
        }
  }

  val submitReturnsFrequency: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          ReturnFrequencyForm.form.bindFromRequest.fold(
            errors => Future.successful(BadRequest(ReturnFrequencyPage(errors))),
            success => returnsService.saveFrequency(success) map { _ =>
              if (success == Frequency.monthly) {
                Redirect(controllers.routes.BankAccountDetailsController.showHasCompanyBankAccountView())
              } else {
                Redirect(routes.ReturnsController.accountPeriodsPage())
              }
            }
          )
        }
  }

  private def startDateGuard(pageVoluntary: Boolean)(intendedLocation: => Future[Result])
                            (implicit hc: HeaderCarrier, currentProfile: CurrentProfile): Future[Result] = {
    returnsService.getThreshold flatMap { documentVoluntary =>
      if (documentVoluntary == pageVoluntary) {
        intendedLocation
      } else {
        Future.successful(
          if (documentVoluntary) {
            Redirect(routes.ReturnsController.voluntaryStartPage())
          } else {
            Redirect(routes.ReturnsController.mandatoryStartPage())
          }
        )
      }
    }
  }

  val voluntaryStartPage: Action[AnyContent] = isAuthenticatedWithProfile { implicit request =>
    implicit profile =>
      ivPassedCheck {
        startDateGuard(pageVoluntary = true) {
          returnsService.voluntaryStartPageViewModel() map { viewModel =>
            implicit val bhs: BankHolidaySet = timeService.bankHolidaySet
            val voluntaryDateForm = VoluntaryDateForm.form(timeService.getMinWorkingDayInFuture, timeService.addMonths(3))
            val filledForm = viewModel.form.fold(voluntaryDateForm) {
              case (dateSelection, date) => voluntaryDateForm.fill((dateSelection, date))
            }
            val dynamicDate = timeService.dynamicFutureDateExample()
            Ok(VoluntaryStartDatePage(filledForm, viewModel.ctActive, dynamicDate))
          }
        }
      }
  }

  val submitVoluntaryStart: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          implicit val bhs: BankHolidaySet = timeService.bankHolidaySet
          val voluntaryDateForm = VoluntaryDateForm.form(timeService.getMinWorkingDayInFuture, timeService.addMonths(3))
          returnsService.retrieveCTActiveDate flatMap { ctActiveDate =>
            voluntaryDateForm.bindFromRequest.fold(
              errors => {
                val dynamicDate = timeService.dynamicFutureDateExample()
                Future.successful(BadRequest(VoluntaryStartDatePage(errors, ctActiveDate, dynamicDate)))
              },
              success => returnsService.saveVoluntaryStartDate(success._1, success._2, ctActiveDate) map redirectBasedOnReclaim
            )
          }
        }
  }

  val mandatoryStartPage: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          startDateGuard(pageVoluntary = false) {
            Future.successful(Ok(MandatoryStartDateConfirmationPage()))
          }
        }
  }

  val submitMandatoryStart: Action[AnyContent] = isAuthenticatedWithProfile {
    implicit request =>
      implicit profile =>
        ivPassedCheck {
          returnsService.saveVatStartDate(None) map redirectBasedOnReclaim
        }
  }

  private def redirectBasedOnReclaim(returns: Returns): Result = {
    if (returns.reclaimVatOnMostReturns.contains(true)) {
      Redirect(routes.ReturnsController.returnsFrequencyPage())
    } else {
      Redirect(routes.ReturnsController.accountPeriodsPage())
    }
  }
}
