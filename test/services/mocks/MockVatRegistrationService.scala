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

package services.mocks

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import services.VatRegistrationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockVatRegistrationService extends MockitoSugar {
  self: Suite =>

  val vatRegistrationServiceMock = mock[VatRegistrationService]

  def mockGetVatScheme(regId: String)(response: Future[JsValue]): OngoingStubbing[Future[JsValue]] =
    when(vatRegistrationServiceMock.getVatSchemeJson(
      ArgumentMatchers.eq(regId)
    )(any[HeaderCarrier])) thenReturn response

  def mockSaveVatScheme(regId: String, partialVatScheme: JsValue)(response: Future[JsValue]): OngoingStubbing[Future[JsValue]] =
    when(vatRegistrationServiceMock.storePartialVatScheme(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(partialVatScheme)
    )(any[HeaderCarrier])) thenReturn response

}
