/*
 * Copyright 2020 HM Revenue & Customs
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

package mocks

import config.AuthClientConnector
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, InsufficientConfidenceLevel, InvalidBearerToken}

import scala.concurrent.Future

trait AuthMock {
  this: MockitoSugar =>

  lazy val mockAuthClientConnector: AuthClientConnector = mock[AuthClientConnector]

  def mockAuthenticated(): OngoingStubbing[Future[Unit]] = {
    when(
      mockAuthClientConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ) thenReturn Future.successful({})
  }

  def mockNotAuthenticated(): OngoingStubbing[Future[Unit]] = {
    when(mockAuthClientConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(new InsufficientConfidenceLevel))
  }

  def mockNoActiveSession(): OngoingStubbing[Future[Unit]] = {
    when(mockAuthClientConnector.authorise[Unit](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(new InvalidBearerToken))
  }

  def mockAuthenticatedOrg(): OngoingStubbing[Future[Option[AffinityGroup]]] = {
    when(
      mockAuthClientConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ) thenReturn Future.successful(Some(Organisation))
  }

  def mockAuthenticatedNonOrg(): OngoingStubbing[Future[Option[AffinityGroup]]] = {
    when(
      mockAuthClientConnector.authorise[Option[AffinityGroup]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ) thenReturn Future.successful(Some(Individual))
  }
}
