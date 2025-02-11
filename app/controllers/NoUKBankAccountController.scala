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
import forms.NoUKBankAccountForm
import javax.inject.{Inject, Singleton}
import models.BankAccount
import play.api.mvc.{Action, AnyContent}
import services.{BankAccountDetailsService, SessionProfile}
import views.html.no_uk_bank_account

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NoUKBankAccountController @Inject()(noUKBankAccountView: no_uk_bank_account,
                                           val authConnector: AuthClientConnector,
                                          val bankAccountDetailsService: BankAccountDetailsService,
                                          val keystoreConnector: KeystoreConnector)
                                         (implicit appConfig: FrontendAppConfig,
                                          val executionContext: ExecutionContext,
                                          baseControllerComponents: BaseControllerComponents)
                                          extends BaseController with SessionProfile {

  val showNoUKBankAccountView: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      _ =>
      Future.successful(Ok(noUKBankAccountView(NoUKBankAccountForm.form)))
  }

  val submitNoUKBankAccount: Action[AnyContent] = isAuthenticatedWithProfile() {
    implicit request =>
      implicit profile => {
        NoUKBankAccountForm().bindFromRequest().fold(
          badForm => Future.successful(BadRequest(noUKBankAccountView(badForm))),
          reason =>
            bankAccountDetailsService.saveBankAccountDetails(BankAccount(isProvided = false, details = None, reason = Some(reason))).map(_ =>
              Redirect(controllers.registration.flatratescheme.routes.JoinFlatRateSchemeController.show())
            )

        )
      }
  }

}
