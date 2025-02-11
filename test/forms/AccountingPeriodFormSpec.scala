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

package forms

import models.Stagger._
import testHelpers.VatRegSpec

class AccountingPeriodFormSpec extends VatRegSpec {

  val form = AccountingPeriodForm.form

  "Binding AccountingPeriodForm" should {
    "Bind successfully for a jan, apr, jul, oct selection" in {
      val data = Map(
        "accountingPeriodRadio" -> "jan"
      )
      form.bind(data).get mustBe jan
    }

    "Bind successfully for a feb, may, aug, nov selection" in {
      val data = Map(
        "accountingPeriodRadio" -> "feb"
      )
      form.bind(data).get mustBe feb
    }

    "Bind successfully for a mar, jun, sep, dec selection" in {
      val data = Map(
        "accountingPeriodRadio" -> "mar"
      )
      form.bind(data).get mustBe mar
    }

    "Fail to bind successfully for an invalid selection" in {
      val data = Map(
        "accountingPeriodRadio" -> "invalidSelection"
      )
      val bound = form.bind(data)
      bound.errors.size         mustBe 1
      bound.errors.head.key     mustBe "accountingPeriodRadio"
      bound.errors.head.message mustBe "validation.accounting.period.missing"
    }

    "Fail to bind successfully if empty" in {
      val data = Map(
        "accountingPeriodRadio" -> ""
      )
      val bound = form.bind(data)
      bound.errors.size         mustBe 1
      bound.errors.head.key     mustBe "accountingPeriodRadio"
      bound.errors.head.message mustBe "validation.accounting.period.missing"
    }
  }
}
