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
import connectors.{ConfigConnector, KeystoreConnector}
import forms._
import forms.genericForms.{YesOrNoAnswer, YesOrNoFormFactory}
import play.api.data.Form
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.time.workingdays.BankHolidaySet
import views.html._

import java.text.DecimalFormat
import java.util.MissingResourceException
import javax.inject.{Inject, Singleton}
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FlatRateController @Inject()(val flatRateService: FlatRateService,
                                   val vatRegistrationService: VatRegistrationService,
                                   val authConnector: AuthClientConnector,
                                   val keystoreConnector: KeystoreConnector,
                                   val configConnector: ConfigConnector,
                                   val timeService: TimeService,
                                   val sicAndComplianceService: SicAndComplianceService,
                                   frs_your_flat_rate: frs_your_flat_rate)
                                  (implicit appConfig: FrontendAppConfig,
                                   val executionContext: ExecutionContext,
                                   baseControllerComponents: BaseControllerComponents)
  extends BaseController with SessionProfile {

  val registerForFrsForm: Form[YesOrNoAnswer] = YesOrNoFormFactory.form("registerForFrsRadio")("frs.registerFor")
  val joinFrsForm: Form[YesOrNoAnswer] = YesOrNoFormFactory.form("joinFrs")("frs.join")
  val yourFlatRateForm: Form[YesOrNoAnswer] = YesOrNoFormFactory.form()("frs.registerForWithSector")
  val overBusinessGoodsForm: Form[Boolean] = OverBusinessGoodsForm.form

  def overBusinessGoodsPercentForm(formPct: Long = 0): Form[Boolean] = new OverBusinessGoodsPercentForm {
    override val pct: Long = formPct
  }.form

  lazy val groupingBusinessTypesValues: ListMap[String, Seq[(String, String)]] = ListMap(configConnector.businessTypes.map { jsObj =>
    (
      (jsObj \ "groupLabel").as[String],
      (jsObj \ "categories").as[Seq[JsObject]].map(js => ((js \ "id").as[String], (js \ "businessType").as[String]))
    )
  }.sortBy(_._1): _*)

  lazy val businessTypeIds: Seq[String] = groupingBusinessTypesValues.values.toSeq.flatMap(radioValues => radioValues map Function.tupled((id, _) => id))

  def annualCostsInclusivePage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate map { flatRateScheme =>
          val viewForm = flatRateScheme.overBusinessGoods.fold(overBusinessGoodsForm)(overBusinessGoodsForm.fill)
          Ok(views.html.annual_costs_inclusive(viewForm))
        }
  }

  def submitAnnualInclusiveCosts: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        overBusinessGoodsForm.bindFromRequest().fold(
          badForm => Future.successful(BadRequest(views.html.annual_costs_inclusive(badForm))),
          view => flatRateService.saveOverBusinessGoods(view) map { _ =>
            if (!view) {
              Redirect(controllers.routes.FlatRateController.registerForFrsPage())
            } else {
              Redirect(controllers.routes.FlatRateController.estimateTotalSales())
            }
          }
        )
  }

  def annualCostsLimitedPage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate map { flatRateScheme =>
          val form = overBusinessGoodsPercentForm(flatRateService.applyPercentRoundUp(flatRateScheme.estimateTotalSales.get))
          val viewForm = flatRateScheme.overBusinessGoodsPercent.fold(form)(form.fill)

          Ok(views.html.annual_costs_limited(viewForm, flatRateService.applyPercentRoundUp(flatRateScheme.estimateTotalSales.get)))
        }
  }

  def submitAnnualCostsLimited: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate flatMap { flatRateScheme =>
          val form = overBusinessGoodsPercentForm(flatRateService.applyPercentRoundUp(flatRateScheme.estimateTotalSales.get))
          form.bindFromRequest().fold(formErr => {
            Future.successful(BadRequest(views.html.annual_costs_limited(formErr, flatRateService.applyPercentRoundUp(flatRateScheme.estimateTotalSales.get))))
          },
            view => flatRateService.saveOverBusinessGoodsPercent(view) map { _ =>
              if (!view) {
                Redirect(controllers.routes.FlatRateController.registerForFrsPage())
              } else {
                Redirect(controllers.routes.FlatRateController.confirmSectorFrsPage())
              }
            }
          )
        }
  }

  def confirmSectorFrsPage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.retrieveSectorPercent map { view =>
          val (_, sector, pct) = view
          Ok(views.html.frs_confirm_business_sector((sector, pct)))
        } recover {
          case _: MissingResourceException => Redirect(controllers.routes.FlatRateController.businessType(true))
        }
  }

  def submitConfirmSectorFrs: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.saveConfirmSector map { _ =>
          Redirect(controllers.routes.FlatRateController.yourFlatRatePage())
        }
  }

  def frsStartDatePage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        for {
          vatStartDate <- flatRateService.fetchVatStartDate
          (choOpt, date) <- flatRateService.getPrepopulatedStartDate(vatStartDate)
        } yield {
          implicit val bhs: BankHolidaySet = timeService.bankHolidaySet
          val dynamicDate = timeService.dynamicFutureDateExample()
          val viewForm = choOpt.fold(FRSStartDateForm.form(timeService.futureWorkingDate(3)))(choice => FRSStartDateForm.form(timeService.futureWorkingDate(3)).fill((choice, date)))
          Ok(views.html.frs_start_date(viewForm, dynamicDate, vatStartDate))
        }
  }

  def submitFrsStartDate: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.fetchVatStartDate flatMap { vatStartDate =>
          implicit val bhs: BankHolidaySet = timeService.bankHolidaySet
          FRSStartDateForm.form(timeService.futureWorkingDate(3), vatStartDate).bindFromRequest().fold(
            badForm => {
              val dynamicDate = timeService.dynamicFutureDateExample()
              Future.successful(BadRequest(views.html.frs_start_date(badForm, dynamicDate, vatStartDate)))
            },
            view => {
              val (choice, date) = view
              flatRateService.saveStartDate(choice, date) map { _ =>
                Redirect(controllers.routes.SummaryController.show())
              }
            }
          )
        }
  }

  def registerForFrsPage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate map { flatRateScheme =>
          val form = flatRateScheme.useThisRate match {
            case Some(useRate) => registerForFrsForm.fill(YesOrNoAnswer(useRate))
            case None => registerForFrsForm
          }
          Ok(views.html.frs_register_for(form))
        }
  }

  def submitRegisterForFrs: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        registerForFrsForm.bindFromRequest().fold(
          badForm => Future.successful(BadRequest(views.html.frs_register_for(badForm))),
          view => flatRateService.saveRegister(view.answer) map { _ =>
            if (view.answer) {
              Redirect(controllers.routes.FlatRateController.frsStartDatePage())
            } else {
              Redirect(controllers.routes.SummaryController.show())
            }
          }
        )
  }

  def yourFlatRatePage: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate flatMap { flatRateScheme =>
          val form = flatRateScheme.useThisRate match {
            case Some(useRate) => yourFlatRateForm.fill(YesOrNoAnswer(useRate))
            case None => yourFlatRateForm
          }
          flatRateService.retrieveSectorPercent map { sectorInfo =>
            val decimalFormat = new DecimalFormat("#0.##")
            val (_, sector, pct) = sectorInfo
            Ok(frs_your_flat_rate(sector, decimalFormat.format(pct), form))
          }
        }
  }

  def submitYourFlatRate: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        yourFlatRateForm.bindFromRequest().fold(
          badForm => flatRateService.retrieveSectorPercent map { view =>
            val decimalFormat = new DecimalFormat("#0.##")
            val (_, sector, pct) = view
            BadRequest(frs_your_flat_rate(sector, decimalFormat.format(pct), badForm))
          },
          view => for {
            _ <- flatRateService.saveUseFlatRate(view.answer)
          } yield if (view.answer) {
            Redirect(controllers.routes.FlatRateController.frsStartDatePage())
          } else {
            Redirect(controllers.routes.SummaryController.show())
          }
        )
  }

  def estimateTotalSales: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        flatRateService.getFlatRate map { flatRateScheme =>
          val form = flatRateScheme.estimateTotalSales.fold(EstimateTotalSalesForm.form)(v => EstimateTotalSalesForm.form.fill(v))
          Ok(views.html.estimate_total_sales(form))
        }
  }

  def submitEstimateTotalSales: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        EstimateTotalSalesForm.form.bindFromRequest().fold(
          badForm => Future.successful(BadRequest(views.html.estimate_total_sales(badForm))),
          data => flatRateService.saveEstimateTotalSales(data) map {
            _ => Redirect(controllers.routes.FlatRateController.annualCostsLimitedPage())
          }
        )
  }

  def businessType(sendGA: Boolean = false): Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        for {
          flatRateScheme <- flatRateService.getFlatRate
          sicAndCompliance <- sicAndComplianceService.getSicAndCompliance
          form = ChooseBusinessTypeForm.form(businessTypeIds)
          formFilled = flatRateScheme.categoryOfBusiness.fold(form)(v => form.fill(v))
          sendGAText = if (sendGA) sicAndCompliance.mainBusinessActivity.map(_.id) else None
        } yield {
          Ok(views.html.chooseBusinessType(formFilled, groupingBusinessTypesValues, sendGAText))
        }
  }

  def submitBusinessType: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile =>
        ChooseBusinessTypeForm.form(businessTypeIds).bindFromRequest().fold(
          badForm => Future.successful(BadRequest(views.html.chooseBusinessType(badForm, groupingBusinessTypesValues))),
          data => flatRateService.saveBusinessType(data) map {
            _ => Redirect(controllers.routes.FlatRateController.yourFlatRatePage())
          }
        )
  }
}
