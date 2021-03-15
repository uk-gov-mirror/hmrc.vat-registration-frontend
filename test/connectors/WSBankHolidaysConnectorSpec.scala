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

package connectors

import org.joda.time.LocalDate
import testHelpers.VatRegSpec
import uk.gov.hmrc.time.workingdays.{BankHoliday, BankHolidaySet}

import scala.concurrent.Future

class WSBankHolidaysConnectorSpec extends VatRegSpec {

  lazy val testConnector = new WSBankHolidaysConnector(mockHttpClient, mockServicesConfig) {
    override lazy val url: String = "test-url"
  }

  "bankHolidays" must {
    "return set of bank holidays for a specified division" in {
      val testHolidaySet = Map(
        "division1" -> BankHolidaySet("division1", List(
          BankHoliday("one", LocalDate.parse("2017-3-22")))),
        "division2" -> BankHolidaySet("division2", List(
          BankHoliday("one", LocalDate.parse("2017-3-22")),
          BankHoliday("another", LocalDate.parse("2017-3-23"))))
      )

      mockHttpGET("test-url", Future.successful(testHolidaySet))

      testConnector.bankHolidays("division1") returns BankHolidaySet("division1", List(
        BankHoliday("one", LocalDate.parse("2017-3-22"))))
    }
  }
}
