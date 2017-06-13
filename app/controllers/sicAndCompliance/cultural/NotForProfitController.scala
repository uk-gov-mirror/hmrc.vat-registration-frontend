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

package controllers.sicAndCompliance.cultural

import javax.inject.Inject

import controllers.CommonPlayDependencies
import controllers.sicAndCompliance.ComplianceExitController
import forms.sicAndCompliance.cultural.NotForProfitForm
import models.view.sicAndCompliance.cultural.NotForProfit
import models.{FinancialCompliancePath, LabourCompliancePath}
import play.api.mvc.{Action, AnyContent}
import services.{S4LService, VatRegistrationService}


class NotForProfitController @Inject()(ds: CommonPlayDependencies)
                                      (implicit s4LService: S4LService, vrs: VatRegistrationService)
  extends ComplianceExitController(ds) {

  val form = NotForProfitForm.form

  def show: Action[AnyContent] = authorised.async(implicit user => implicit request =>
    viewModel[NotForProfit]().fold(form)(form.fill)
      .map(f => Ok(views.html.pages.sicAndCompliance.cultural.not_for_profit(f))))

  def submit: Action[AnyContent] = authorised.async(implicit user => implicit request =>
    form.bindFromRequest().fold(
      badForm => BadRequest(views.html.pages.sicAndCompliance.cultural.not_for_profit(badForm)).pure,
      data => for {
        clearCompliance <- clearComplianceContainer
        _ <- save(clearCompliance)
        _ <- save(data)
        _ <- vrs.deleteElements(List(FinancialCompliancePath, LabourCompliancePath))
        call <- submitAndExit
      } yield Redirect(call)))

}
