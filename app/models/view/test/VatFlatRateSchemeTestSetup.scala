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

package models.view.test

import play.api.libs.json.Json

case class VatFlatRateSchemeTestSetup(joinFrs: Option[String],
                                      annualCostsInclusive: Option[String],
                                      annualCostsLimited: Option[String],
                                      registerForFrs: Option[String],
                                      frsStartDateChoice: Option[String],
                                      frsStartDateDay: Option[String],
                                      frsStartDateMonth: Option[String],
                                      frsStartDateYear: Option[String])

object VatFlatRateSchemeTestSetup {
  implicit val format = Json.format[VatFlatRateSchemeTestSetup]
}











