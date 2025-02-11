
package controllers

import fixtures.ApplicantDetailsFixture
import itutil.{ControllerISpec, IntegrationSpecBase}
import models.api.Threshold
import models.view.ApplicantDetails
import models.{DateSelection, Returns, Start}
import org.joda.time.LocalDate
import org.scalatest.concurrent.IntegrationPatience
import play.api.libs.json.Json
import play.api.test.Helpers._
import support.AppAndStubs

class ReturnsControllerISpec extends ControllerISpec {

  "GET /vat-start-date" must {
    "Return OK when the user is authenticated" in {
      given()
        .user.isAuthorised
        .s4lContainer[Returns].contains(Returns(None, None, None, None, Some(Start(Some(testApplicantIncorpDate)))))
        .s4lContainer[ApplicantDetails].contains(validFullApplicantDetails)
        .vatScheme.has("threshold-data", Json.toJson(Threshold(mandatoryRegistration = false)))

      val res = buildClient("/vat-start-date").get()

      whenReady(res) { result =>
        result.status mustBe OK
      }
    }

    "Return INTERNAL_SERVER_ERROR when the user is not authenticated" in {
      /* TODO: It SHOULD return UNAUTHORIZED, but we need to alter how the standard error template is
          rendered first as the way it is currently would cause user confusion */
      given()
        .user.isNotAuthorised

      val res = buildClient("/vat-start-date").get()

      whenReady(res) { result =>
        result.status mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "POST /vat-start-date" must {
    "Redirect to the next page when all data is valid" in {
      val today = LocalDate.now().plusDays(1)
      val applicantJson = Json.toJson(validFullApplicantDetails)(ApplicantDetails.apiFormat)

      given()
        .user.isAuthorised
        .s4lContainer[Returns].isUpdatedWith(Returns(None, None, None, None, Some(Start(Some(java.time.LocalDate.parse(today.toString))))))
        .s4lContainer[ApplicantDetails].isUpdatedWith(validFullApplicantDetails)
        .vatScheme.has("threshold-data", Json.toJson(Threshold(mandatoryRegistration = false)))
        .vatScheme.has("applicant-details", applicantJson)
        .vatScheme.patched("applicant-details", applicantJson)

      val res = buildClient("/vat-start-date").post(Json.obj(
        "startDateRadio" -> DateSelection.specific_date,
        "startDate" -> Json.obj(
          "day" -> today.getDayOfMonth.toString,
          "month" -> today.getMonthOfYear.toString,
          "year" -> today.getYear.toString
        )
      ))

      whenReady(res) { result =>
        result.status mustBe SEE_OTHER
      }
    }
  }

}
