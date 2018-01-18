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

package controllers.sicAndCompliance.labour

import controllers.sicAndCompliance
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models.S4LVatSicAndCompliance
import models.view.sicAndCompliance.labour.Workers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class WorkersControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  object WorkersController extends WorkersController(
    ds,
    mockKeystoreConnector,
    mockAuthConnector,
    mockS4LService,
    mockVatRegistrationService
  )

  val fakeRequest = FakeRequest(sicAndCompliance.labour.routes.WorkersController.show())

  s"GET ${sicAndCompliance.labour.routes.WorkersController.show()}" should {
    "return HTML when there's a Workers model in S4L" in {
      save4laterReturnsViewModel(Workers(5))()
      mockGetCurrentProfile()
      submitAuthorised(WorkersController.show(), fakeRequest.withFormUrlEncodedBody(
        "numberOfWorkers" -> "5"
      )) {
        result =>
          status(result) mustBe OK
          contentType(result) mustBe Some("text/html")
          charset(result) mustBe Some("utf-8")
          contentAsString(result) must include("How many workers does the company provide at any one time?")
      }
    }

    "return HTML when there's nothing in S4L and vatScheme contains data" in {
      save4laterReturnsNoViewModel[Workers]()
      when(mockVatRegistrationService.getVatScheme(any(), any())).thenReturn(Future.successful(validVatScheme))
      mockGetCurrentProfile()
      callAuthorised(WorkersController.show) {
        result =>
          status(result) mustBe OK
          contentType(result) mustBe Some("text/html")
          charset(result) mustBe Some("utf-8")
          contentAsString(result) must include("How many workers does the company provide at any one time?")
      }
    }

  "return HTML when there's nothing in S4L and vatScheme contains no data" in {
    save4laterReturnsNoViewModel[Workers]()
    when(mockVatRegistrationService.getVatScheme(any(), any[HeaderCarrier]())).thenReturn(Future.successful(emptyVatScheme))
    mockGetCurrentProfile()
      callAuthorised(WorkersController.show) {
        result =>
          status(result) mustBe OK
          contentType(result) mustBe Some("text/html")
          charset(result) mustBe Some("utf-8")
          contentAsString(result) must include("How many workers does the company provide at any one time?")
      }
    }
  }

  s"POST ${sicAndCompliance.labour.routes.WorkersController.submit()}" should {
    "return 400 with Empty data" in {
      mockGetCurrentProfile()
      submitAuthorised(WorkersController.submit(), fakeRequest.withFormUrlEncodedBody(
      )) {
        result => status(result) mustBe Status.BAD_REQUEST
      }
    }

    "return 303 with less than 8 workers entered" in {
      when(mockVatRegistrationService.submitSicAndCompliance(any(), any())).thenReturn(Future.successful(validSicAndCompliance))
      when(mockS4LService.save(any())(any(), any(), any(), any())).thenReturn(dummyCacheMap.pure)
      save4laterExpectsSave[Workers]()
      save4laterReturns(S4LVatSicAndCompliance())
      mockGetCurrentProfile()
      submitAuthorised(WorkersController.submit(), fakeRequest.withFormUrlEncodedBody(
        "numberOfWorkers" -> "5"
      )) {
        result =>
          result redirectsTo s"$contextRoot/trading-name"
      }
    }

    "return 303 with 8 or more workers entered" in {
      when(mockVatRegistrationService.submitSicAndCompliance(any(), any())).thenReturn(Future.successful(validSicAndCompliance))
      save4laterExpectsSave[Workers]()
      mockGetCurrentProfile()
      submitAuthorised(WorkersController.submit(), fakeRequest.withFormUrlEncodedBody(
        "numberOfWorkers" -> "8"
      )) {
        result =>
          result redirectsTo s"$contextRoot/provides-workers-on-temporary-contracts"
      }
    }
  }
}
