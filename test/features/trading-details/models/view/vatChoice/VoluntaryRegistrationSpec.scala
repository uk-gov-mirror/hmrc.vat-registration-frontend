/*
 * Copyright 2018 HM Revenue & Customs
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

package models.view.vatTradingDetails.vatChoice

import fixtures.VatRegistrationFixture
import models.S4LVatEligibilityChoice
import uk.gov.hmrc.play.test.UnitSpec

class VoluntaryRegistrationSpec extends UnitSpec with VatRegistrationFixture {

  "ViewModelFormat" should {
    val validVoluntaryRegistration = VoluntaryRegistration(VoluntaryRegistration.REGISTER_YES)
    val s4LEligibilityChoice: S4LVatEligibilityChoice = S4LVatEligibilityChoice(voluntaryRegistration = Some(validVoluntaryRegistration))

    "extract voluntaryRegistration from vatTradingDetails" in {
      VoluntaryRegistration.viewModelFormat.read(s4LEligibilityChoice) shouldBe Some(validVoluntaryRegistration)
    }

    "update empty vatContact with voluntaryRegistration" in {
      VoluntaryRegistration.viewModelFormat.update(validVoluntaryRegistration, Option.empty[S4LVatEligibilityChoice]).voluntaryRegistration shouldBe Some(validVoluntaryRegistration)
    }

    "update non-empty vatContact with voluntaryRegistration" in {
      VoluntaryRegistration.viewModelFormat.update(validVoluntaryRegistration, Some(s4LEligibilityChoice)).voluntaryRegistration shouldBe Some(validVoluntaryRegistration)
    }

  }

}
