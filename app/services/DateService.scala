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

package services

import java.time.LocalDate

import common.DateConversions._
import connectors.{FallbackBankHolidaysConnector, WSBankHolidaysConnector}
import javax.inject.{Inject, Singleton}
import play.api.cache.SyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.workingdays._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

@Singleton
class DateService @Inject()(val bankHolidaysConnector: WSBankHolidaysConnector,
                            val cache: SyncCacheApi,
                            val fallbackBHConnector: FallbackBankHolidaysConnector) {

  private val BANK_HOLIDAYS_CACHE_KEY = "bankHolidaySet"

  private val defaultHolidaySet: BankHolidaySet = Await.result(fallbackBHConnector.bankHolidays(), 1 second)

  def addWorkingDays(date: LocalDate, days: Int): LocalDate = {
    implicit val hols: BankHolidaySet = cache.getOrElseUpdate[BankHolidaySet](BANK_HOLIDAYS_CACHE_KEY, 1 day) {
      logger.info(s"Reloading cache entry for $BANK_HOLIDAYS_CACHE_KEY")
      Try {
        Await.result(bankHolidaysConnector.bankHolidays()(HeaderCarrier()), 5 seconds)
      }.getOrElse {
        logger.error("Failed to load bank holidays schedule from BankHolidaysConnector, using default bank holiday set")
        defaultHolidaySet
      }
    }

    (date: org.joda.time.LocalDate).plusWorkingDays(days)
  }
}
