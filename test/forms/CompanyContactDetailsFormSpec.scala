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

package forms

import helpers.FormInspectors._
import models.CompanyContactDetails
import play.api.data.FormError
import testHelpers.VatRegSpec

class CompanyContactDetailsFormSpec extends VatRegSpec {

  val testForm = CompanyContactDetailsForm.form


  "A business contact details form" must {

    val EMAIL = "some@email.com"
    val DAYTIME_PHONE = "0123456789"
    val MOBILE = "9876543210"
    val WEBSITE = "http://www.some.website.com/"
    val ALTERNATE_MOBILE = "+449876543210"
    val INTERNATIONAL_NUMBER = "(123) 456-7890"

    "be valid" when {

      "email and mobile number is provided" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq(MOBILE))
        testForm.bindFromRequest(data) shouldContainValue CompanyContactDetails(EMAIL, None, Some(MOBILE), None)
      }

      "email and alternate number is provided" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq(ALTERNATE_MOBILE))
        testForm.bindFromRequest(data) shouldContainValue CompanyContactDetails(EMAIL, None, Some(ALTERNATE_MOBILE), None)
      }

      "email and international number is provided" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq(INTERNATIONAL_NUMBER))
        testForm.bindFromRequest(data) shouldContainValue CompanyContactDetails(EMAIL, None, Some(INTERNATIONAL_NUMBER.replaceAll(" ", "")), None)
      }

      "email and phone number is provided" in {
        val data = Map("email" -> Seq(EMAIL), "daytimePhone" -> Seq(DAYTIME_PHONE))
        testForm.bindFromRequest(data) shouldContainValue CompanyContactDetails(EMAIL, Some(DAYTIME_PHONE), None, None)
      }

      "email and both phone numbers are provided" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq(MOBILE), "daytimePhone" -> Seq(DAYTIME_PHONE))
        testForm.bindFromRequest(data) shouldContainValue CompanyContactDetails(
          email          = EMAIL,
          phoneNumber    = Some(DAYTIME_PHONE),
          mobileNumber   = Some(MOBILE),
          websiteAddress = None
        )
      }

      "all fields filled in" in {

        val data = Map(
          "email" -> Seq(EMAIL),
          "mobile" -> Seq(MOBILE),
          "daytimePhone" -> Seq(DAYTIME_PHONE),
          "website" -> Seq(WEBSITE))

        val form = testForm.bindFromRequest(data)

        form shouldContainValue CompanyContactDetails(EMAIL, Some(DAYTIME_PHONE), Some(MOBILE), Some(WEBSITE))
      }

      "any additional values are submitted" in {
        val data: Map[String, Seq[String]] = Map(
          "email" -> Seq(EMAIL),
          "daytimePhone" -> Seq(DAYTIME_PHONE),
          "foo" -> Seq("bar"))

        val form = testForm.bindFromRequest(data)

        form shouldContainValue CompanyContactDetails(EMAIL, Some(DAYTIME_PHONE), None, None)
      }

    }

    "be rejected with appropriate error messages" when {

      "no phone number is provided, just email" in {
        val data = Map("email" -> Seq(EMAIL))
        val form = testForm.bindFromRequest(data)
        form shouldHaveGlobalErrors "validation.businessContactDetails.atLeastOneNumber.missing"
      }

      "invalid mobile phone number is provided and email" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq("invalid phone number"))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("mobile" -> "validation.invalid.businessContactDetails")
      }

      "invalid daytime phone number is provided and email" in {
        val data = Map("email" -> Seq(EMAIL), "daytimePhone" -> Seq("invalid phone number"))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("daytimePhone" -> "validation.invalid.businessContactDetails")
      }

      "too long mobile phone number is provided and email" in {
        val data = Map("email" -> Seq(EMAIL), "mobile" -> Seq("1234567890123456789011234"))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("mobile" -> "validation.invalid.businessContactDetails.tooLong")
      }

      "too long daytime phone number is provided and email" in {
        val data = Map("email" -> Seq(EMAIL), "daytimePhone" -> Seq("1234567890123456789121234"))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("daytimePhone" -> "validation.invalid.businessContactDetails.tooLong")
      }

      "no email is provided" in {
        val data = Map("daytimePhone" -> Seq(DAYTIME_PHONE))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("email" -> "validation.businessContactDetails.email.missing")
      }

      "blank email is provided" in {
        val data = Map("email" -> Seq(""), "mobile" -> Seq(MOBILE))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("email" -> "validation.businessContactDetails.email.missing")
      }

      "invalid email is provided" in {
        val data = Map("email" -> Seq("some invalid email"), "daytimePhone" -> Seq(DAYTIME_PHONE))
        val form = testForm.bindFromRequest(data)
        form shouldHaveErrors Seq("email" -> "validation.businessContactDetails.email.invalid")
      }
    }
  }

  "Calling transformErrors" must {
    "return a form with only one global error when the form has errors and data is empty" in {
      val data = Map("email" -> Seq(""), "mobile" -> Seq(""), "csrfToken" -> Seq(""))
      val formError = testForm.bindFromRequest(data)

      CompanyContactDetailsForm.transformErrors(formError).hasErrors shouldBe true
      CompanyContactDetailsForm.transformErrors(formError).hasGlobalErrors shouldBe true
      CompanyContactDetailsForm.transformErrors(formError).globalError shouldBe Some(FormError("", "validation.businessContactDetails.missing", Seq("businessContactDetails")))
    }

    "do nothing if form has errors and data is not empty" in {
      val data = Map("email" -> Seq("t@@"), "mobile" -> Seq(""), "csrfToken" -> Seq(""))
      val formError = testForm.bindFromRequest(data)

      CompanyContactDetailsForm.transformErrors(formError) shouldBe formError
    }
  }
}
