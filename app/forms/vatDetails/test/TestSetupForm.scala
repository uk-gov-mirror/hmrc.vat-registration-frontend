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

package forms.vatDetails.test

import models.view.TestSetup
import play.api.data.Form
import play.api.data.Forms._

object TestSetupForm {

  val form = Form(
    mapping(
      "taxableTurnoverChoice" -> optional(text),
      "voluntaryChoice" -> optional(text),
      "startDateChoice" -> optional(text),
      "startDateDay" -> optional(text),
      "startDateMonth" -> optional(text),
      "startDateYear" -> optional(text),
      "tradingNameChoice" -> optional(text),
      "tradingName" -> optional(text),
      "businessActivityDescription" -> optional(text),
      "companyBankAccountChoice" -> optional(text),
      "companyBankAccountName" -> optional(text),
      "companyBankAccountNumber" -> optional(text),
      "sortCode" -> optional(text),
      "estimateVatTurnover" -> optional(text),
      "zeroRatedSalesChoice" -> optional(text),
      "zeroRatedSalesEstimate" -> optional(text),
      "vatChargeExpectancyChoice" -> optional(text),
      "vatReturnFrequency" -> optional(text),
      "accountingPeriod" -> optional(text)
    )(TestSetup.apply)(TestSetup.unapply)
  )

}
