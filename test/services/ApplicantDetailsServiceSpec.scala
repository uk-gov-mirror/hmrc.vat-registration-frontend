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

import common.enums.VatRegStatus
import fixtures.ApplicantDetailsFixtures
import models.{CurrentProfile, TelephoneNumber}
import models.api.Address
import models.external.{EmailAddress, EmailVerified}
import models.view._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import testHelpers.VatRegSpec
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

class ApplicantDetailsServiceSpec extends VatRegSpec with ApplicantDetailsFixtures {
  override val testRegId = "testRegId"

  override implicit val currentProfile = CurrentProfile(testRegId, VatRegStatus.draft)

  val validFullApplicantDetailsNoFormerName = completeApplicantDetails.copy(
    formerName = Some(FormerNameView(false, None)),
    formerNameDate = None
  )

  val jsonPartialApplicantDetails = Json.parse(
    s"""
       |{
       |  "name": {
       |    "first": "First",
       |    "middle": "Middle",
       |    "last": "Last"
       |  },
       |  "role": "Director",
       |  "dob": "1998-07-12",
       |  "nino": "SR123456Z"
       |}
       """.stripMargin)

  class Setup(s4lData: Option[ApplicantDetails] = None, backendData: Option[JsValue] = None) {
    val service = new ApplicantDetailsService(
      mockVatRegistrationConnector,
      mockS4LService
    )

    when(mockS4LService.fetchAndGetNoAux[ApplicantDetails](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(s4lData))

    when(mockVatRegistrationConnector.getApplicantDetails(any())(any())).thenReturn(Future.successful(backendData))

    when(mockS4LService.saveNoAux(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(CacheMap("", Map())))
  }

  class SetupForS4LSave(applicantDetails: ApplicantDetails = emptyApplicantDetails) {
    val service: ApplicantDetailsService = new ApplicantDetailsService(
      mockVatRegistrationConnector,
      mockS4LService
    ) {
      override def getApplicantDetails(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[ApplicantDetails] = {
        Future.successful(applicantDetails)
      }
    }

    when(mockS4LService.saveNoAux(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(CacheMap("", Map())))
  }

  class SetupForBackendSave(applicantDetails: ApplicantDetails = emptyApplicantDetails) {
    val service: ApplicantDetailsService = new ApplicantDetailsService(
      mockVatRegistrationConnector,
      mockS4LService
    ) {
      override def getApplicantDetails(implicit cp: CurrentProfile, hc: HeaderCarrier): Future[ApplicantDetails] = {
        Future.successful(applicantDetails)
      }
    }

    when(mockVatRegistrationConnector.patchApplicantDetails(any())(any(), any()))
      .thenReturn(Future.successful(Json.toJson(applicantDetails)))

    when(mockS4LService.clear(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(HttpResponse(200)))
  }

  "Calling getApplicantDetails" should {
    val jsonFullApplicantDetailsWithEmail = Json.parse(
      s"""
         |{
         |  "name": {
         |    "first": "First",
         |    "middle": "Middle",
         |    "last": "Last"
         |  },
         |  "role": "Director",
         |  "dob": "1998-07-12",
         |  "nino": "SR123456Z",
         |  "currentAddress": {
         |    "line1": "TestLine1",
         |    "line2": "TestLine2",
         |    "postcode": "TE 1ST",
         |    "addressValidated": true
         |  },
         |  "contact": {
         |    "email": "test@t.test",
         |    "emailVerified": true
         |  }
         |}
       """.stripMargin)

    "return a default ApplicantDetails view model if nothing is in S4L & backend" in new Setup {
      service.getApplicantDetails returns emptyApplicantDetails
    }

    "return a partial ApplicantDetails view model from backend" in new Setup(None, Some(jsonPartialApplicantDetails)) {
      val expected = ApplicantDetails(
        homeAddress = None,
        emailAddress = None,
        emailVerified = None,
        telephoneNumber = None,
        formerName = Some(FormerNameView(false, None)),
        formerNameDate = None,
        previousAddress = Some(PreviousAddressView(true, None))
      )
      service.getApplicantDetails returns expected
    }

    "return a full ApplicantDetails view model from backend with an email" in new Setup(None, Some(jsonFullApplicantDetailsWithEmail)) {
      val currentAddress = Address(line1 = "TestLine1", line2 = "TestLine2", postcode = Some("TE 1ST"), addressValidated = true)
      val expected: ApplicantDetails = ApplicantDetails(
        homeAddress = Some(HomeAddressView(currentAddress.id, Some(currentAddress))),
        emailAddress = Some(EmailAddress("test@t.test")),
        emailVerified = Some(EmailVerified(true)),
        telephoneNumber = None,
        formerName = Some(FormerNameView(false, None)),
        formerNameDate = None,
        previousAddress = Some(PreviousAddressView(true, None))
      )
      service.getApplicantDetails returns expected
    }

    "return a full ApplicantDetails view model from backend without an email" in new Setup(None, Some(Json.toJson(completeApplicantDetails)(ApplicantDetails.apiWrites))) {
      val currentAddress = Address(line1 = "TestLine1", line2 = "TestLine2", postcode = Some("TE 1ST"), addressValidated = true)

      service.getApplicantDetails returns completeApplicantDetails
    }
  }

  "Calling updateApplicantDetails" should {
    "return a ApplicantDetails" when {
      "updating current address" that {
        val currentAddress = Address(line1 = "Line1", line2 = "Line2", postcode = Some("PO BOX"), addressValidated = true)
        val applicantHomeAddress = HomeAddressView(currentAddress.id, Some(currentAddress))

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(homeAddress = Some(applicantHomeAddress))

          service.saveApplicantDetails(applicantHomeAddress) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails) {
          val expected = completeApplicantDetails.copy(homeAddress = Some(applicantHomeAddress))

          service.saveApplicantDetails(applicantHomeAddress) returns expected
        }
      }

      "updating incorporation details" should {
        "store successfully in S4L if applicant details isn't complete" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(incorporationDetails = Some(testIncorpDetails))

          service.saveApplicantDetails(testIncorpDetails) returns expected
        }
        "store successfully in the backned if applicant details is complete" in new SetupForBackendSave(completeApplicantDetails.copy(incorporationDetails = None)) {
          val expected = completeApplicantDetails

          service.saveApplicantDetails(testIncorpDetails) returns expected
        }
      }

      "updating applicant contact email" that {
        val applicantEmailAddress = EmailAddress("tt@dd.uk")

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(emailAddress = Some(applicantEmailAddress))

          service.saveApplicantDetails(applicantEmailAddress) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails.copy(emailAddress = None)) {
          val expected = completeApplicantDetails.copy(emailAddress = Some(applicantEmailAddress))

          service.saveApplicantDetails(applicantEmailAddress) returns expected
        }
      }

      "updating applicant contact email verified" that {
        val applicantEmailVerified = EmailVerified(true)

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(emailVerified = Some(applicantEmailVerified))

          service.saveApplicantDetails(applicantEmailVerified) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails.copy(emailVerified = None)) {
          val expected = completeApplicantDetails.copy(emailVerified = Some(applicantEmailVerified))

          service.saveApplicantDetails(applicantEmailVerified) returns expected
        }
      }

      "updating applicant contact telephone number" that {
        val applicantTelephoneNumber = TelephoneNumber("1234")

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(telephoneNumber = Some(applicantTelephoneNumber))

          service.saveApplicantDetails(applicantTelephoneNumber) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails.copy(telephoneNumber = None)) {
          val expected = completeApplicantDetails.copy(telephoneNumber = Some(applicantTelephoneNumber))

          service.saveApplicantDetails(applicantTelephoneNumber) returns expected
        }
      }

      "updating applicant former name" that {
        val formerNameFalse = FormerNameView(false, None)
        val formerNameTrue = FormerNameView(true, Some("New FormerName TADA"))

        "makes the block incomplete and save to S4L, model was previously incomplete" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(formerName = Some(formerNameFalse))

          service.saveApplicantDetails(formerNameFalse) returns expected
        }

        "makes the block incomplete and save to S4L, model was previously complete no former name" in new SetupForS4LSave(validFullApplicantDetailsNoFormerName) {
          val formerName = FormerNameView(true, Some("New Name TADA"))
          val expected = validFullApplicantDetailsNoFormerName.copy(formerName = Some(formerName))

          service.saveApplicantDetails(formerName) returns expected
        }

        "makes the block complete with no former name and save to backend" in new SetupForBackendSave(completeApplicantDetails) {
          val expected = completeApplicantDetails.copy(formerName = Some(formerNameFalse), formerNameDate = None)

          service.saveApplicantDetails(formerNameFalse) returns expected
        }

        "update the former name and clean up former name date" in new SetupForS4LSave(completeApplicantDetails.copy(formerName = Some(formerNameTrue), formerNameDate = None)) {
          val expected = completeApplicantDetails.copy(formerName = Some(formerNameTrue), formerNameDate = None)

          service.saveApplicantDetails(formerNameTrue) returns expected
        }
      }

      "updating applicant former name date change" that {
        val formerNameDate = FormerNameDateView(LocalDate.of(2002, 5, 15))

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(formerNameDate = Some(formerNameDate))

          service.saveApplicantDetails(formerNameDate) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails) {
          val expected = completeApplicantDetails.copy(formerNameDate = Some(formerNameDate))

          service.saveApplicantDetails(formerNameDate) returns expected
        }
      }

      "updating applicant previous address" that {
        val addr = Address(line1 = "PrevLine1", line2 = "PrevLine2", postcode = Some("PO PRE"), addressValidated = true)
        val previousAddress = PreviousAddressView(true, Some(addr))

        "makes the block incomplete and save to S4L" in new SetupForS4LSave(emptyApplicantDetails) {
          val expected = emptyApplicantDetails.copy(previousAddress = Some(previousAddress))

          service.saveApplicantDetails(previousAddress) returns expected
        }

        "makes the block complete and save to backend" in new SetupForBackendSave(completeApplicantDetails) {
          val expected = completeApplicantDetails.copy(previousAddress = Some(previousAddress))

          service.saveApplicantDetails(previousAddress) returns expected
        }
      }
    }
  }

}
