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

package utils

import common.enums.VatRegStatus
import connectors.KeystoreConnect
import helpers.VatRegSpec
import models.CurrentProfile
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionProfile

import scala.concurrent.Future


class SessionProfileSpec extends VatRegSpec {

  object TestSession extends SessionProfile {
    override val keystoreConnector: KeystoreConnect = mockKeystoreConnector
  }

  def testFunc : Future[Result] = Future.successful(Ok)
  implicit val request = FakeRequest()

  val validProfile = CurrentProfile("testName", "testRegId", "testTransId", VatRegStatus.draft, None, Some(true))

  "calling withCurrentProfile" should {
    "redirect to the welcome show if the current profile was not fetched from keystore" in {
      mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", None)
      val result = TestSession.withCurrentProfile() { _ => testFunc }
      status(result) mustBe 303
      redirectLocation(result) mustBe Some("/register-for-vat")
    }
    "perform the passed in function" when {
      "the ct status is not present in the current profile" in {
        mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", Some(validProfile))
        val result = TestSession.withCurrentProfile() { _ => testFunc }
        status(result) mustBe OK
      }
      "the ct status does not equal a status 06" in {
        mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", Some(validProfile))
        val result = TestSession.withCurrentProfile() { _ => testFunc }
        status(result) mustBe OK
      }
      "the vat status is held but checkStatus is set to false" in {
        mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", Some(validProfile.copy(vatRegistrationStatus = VatRegStatus.held)))
        val result = TestSession.withCurrentProfile(checkStatus = false) { _ => testFunc }
        status(result) mustBe OK
      }
    }
    "redirect to post sign in if the status is held" in {
      mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", Some(validProfile.copy(vatRegistrationStatus = VatRegStatus.held)))
      val result = TestSession.withCurrentProfile() { _ => testFunc }
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/register-for-vat/post-sign-in")
    }
    "redirect to the retry submission page if the status is locked" in {
      mockKeystoreFetchAndGet[CurrentProfile]("CurrentProfile", Some(validProfile.copy(vatRegistrationStatus = VatRegStatus.locked)))
      val result = TestSession.withCurrentProfile() { _ => testFunc }
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("/register-for-vat/submission-failure")
    }
  }
}
