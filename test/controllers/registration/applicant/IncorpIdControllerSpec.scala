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

package controllers.registration.applicant

import _root_.models._
import controllers.registration.applicant.{routes => applicantRoutes}
import fixtures.VatRegistrationFixture
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import testHelpers.{ControllerSpec, FutureAssertions}

import scala.concurrent.Future
import controllers.registration.applicant.{routes => applicantRoutes}
import featureswitch.core.config.{FeatureSwitching, UseSoleTraderIdentification}
import services.mocks.{MockApplicantDetailsService, TimeServiceMock}

class IncorpIdControllerSpec extends ControllerSpec
  with VatRegistrationFixture
  with TimeServiceMock
  with FutureAssertions
  with MockApplicantDetailsService
  with FeatureSwitching {

  val testJourneyId = "testJourneyId"

  class Setup(cp: Option[CurrentProfile] = Some(currentProfile)) {
    val testController: IncorpIdController = new IncorpIdController(
      mockAuthClientConnector,
      mockKeystoreConnector,
      mockIncorpIdService,
      mockApplicantDetailsService
    )

    mockAuthenticated()
    mockWithCurrentProfile(cp)
  }

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "startIncorpIdJourney" should {
    "redirect to the journeyStartUrl" in new Setup {
      lazy val testJourneyStartUrl = "/test"
      mockCreateJourney(appConfig.incorpIdCallbackUrl, "Register for VAT", "vrs", appConfig.feedbackUrl)(Future.successful(testJourneyStartUrl))

      lazy val res: Future[Result] = testController.startIncorpIdJourney()(fakeRequest)

      status(res) mustBe SEE_OTHER
      redirectLocation(res) mustBe Some(testJourneyStartUrl)
    }
  }

  "incorpIdCallback" should {
    "the UseSoleTraderIdentification feature switch is enabled" should {
      "store the incorporation details and redirect to PDV when the response is valid" in new Setup {
        enable(UseSoleTraderIdentification)
        val onwardUrl = applicantRoutes.SoleTraderIdentificationController.startJourney().url
        mockGetDetails(testJourneyId)(Future.successful(testIncorpDetails))
        mockSaveApplicantDetails(testIncorpDetails)(completeApplicantDetails)

        val res = testController.incorpIdCallback(testJourneyId)(fakeRequest)

        status(res) mustBe SEE_OTHER
        redirectLocation(res) must contain(onwardUrl)
      }
    }
    "the UseSoleTraderIdentification feature switch is disabled" should {
      "store the incorporation details and redirect to PDV when the response is valid" in new Setup {
        disable(UseSoleTraderIdentification)
        val onwardUrl = applicantRoutes.PersonalDetailsValidationController.startPersonalDetailsValidationJourney().url
        mockGetDetails(testJourneyId)(Future.successful(testIncorpDetails))
        mockSaveApplicantDetails(testIncorpDetails)(completeApplicantDetails)

        val res = testController.incorpIdCallback(testJourneyId)(fakeRequest)

        status(res) mustBe SEE_OTHER
        redirectLocation(res) must contain(onwardUrl)
      }
    }
  }

}
