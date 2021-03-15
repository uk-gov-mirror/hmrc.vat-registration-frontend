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

import config.FrontendAppConfig
import play.api.libs.json.Json
import testHelpers.VatRegSpec
import uk.gov.hmrc.http.HttpResponse
import play.api.test.Helpers._


class SoleTraderIdentificationConnectorSpec extends VatRegSpec {

  class Setup {
    implicit val ex = scala.concurrent.ExecutionContext.Implicits.global
    val config = new FrontendAppConfig(mockServicesConfig)
    val connector = new SoleTraderIdentificationConnector(mockHttpClient)(ex, config)
    val createJourneyUrl = "/sole-trader-identification/journey"
  }

  "createJourney" when {
    "the API returns CREATED" must {
      "return the journey ID" in new Setup {
        mockHttpPOST(createJourneyUrl, HttpResponse(CREATED, Some(Json.obj("journeyId" -> "1"))))
        val res = await(connector.createJourney)
        res mustBe "1"
      }
    }
  }

}
