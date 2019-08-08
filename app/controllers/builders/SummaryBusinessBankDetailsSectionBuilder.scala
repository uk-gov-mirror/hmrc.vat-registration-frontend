/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.builders

import common.StringMasking.MaskedStringConverter
import features.bankAccountDetails.models.BankAccount
import models.view.{SummaryRow, SummarySection}

case class SummaryBusinessBankDetailsSectionBuilder(bankAccount: Option[BankAccount]) extends SummarySectionBuilder {

  override val sectionId: String = "bankDetails"

  val accountIsProvidedRow: SummaryRow = SummaryRow(
    s"$sectionId.companyBankAccount",
    bankAccount.map(_.isProvided).fold("app.common.no")(if(_) "app.common.yes" else "app.common.no"),
    Some(features.bankAccountDetails.controllers.routes.BankAccountDetailsController.showHasCompanyBankAccountView())
  )

  val companyBankAccountNameRow: SummaryRow = SummaryRow(
    s"$sectionId.companyBankAccount.name",
    bankAccount.flatMap(_.details).fold("app.common.no")(_.name),
    Some(features.bankAccountDetails.controllers.routes.BankAccountDetailsController.showEnterCompanyBankAccountDetails())
  )

  val companyBankAccountNumberRow: SummaryRow = SummaryRow(
    s"$sectionId.companyBankAccount.number",
    bankAccount.flatMap(_.details).fold("app.common.no")(_.number.mask(4)),
    Some(features.bankAccountDetails.controllers.routes.BankAccountDetailsController.showEnterCompanyBankAccountDetails())
  )

  val companyBankAccountSortCodeRow: SummaryRow = SummaryRow(
    s"$sectionId.companyBankAccount.sortCode",
    bankAccount.flatMap(_.details).fold("app.common.no")(_.sortCode),
    Some(features.bankAccountDetails.controllers.routes.BankAccountDetailsController.showEnterCompanyBankAccountDetails())
  )


  val section: SummarySection = SummarySection(
    sectionId,
    Seq(
      (accountIsProvidedRow, true),
      (companyBankAccountNameRow, bankAccount.map(_.details).isDefined),
      (companyBankAccountNumberRow, bankAccount.map(_.details).isDefined),
      (companyBankAccountSortCodeRow, bankAccount.map(_.details).isDefined)
    )
  )
}