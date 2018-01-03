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

package forms.vatContact

import forms.FormValidation._
import models.view.vatContact.BusinessContactDetails
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

object BusinessContactDetailsForm {

  val EMAIL_MAX_LENGTH = 70
  val EMAIL_PATTERN = """^([A-Za-z0-9\-_.]+)@([A-Za-z0-9\-_.]+)\.[A-Za-z0-9\-_.]{2,3}$""".r
  val PHONE_NUMBER_PATTERN = """[\d]{1,20}""".r

  private val FORM_NAME = "businessContactDetails"

  private val EMAIL = "email"
  private val DAYTIME_PHONE = "daytimePhone"
  private val MOBILE = "mobile"
  private val WEBSITE = "website"

  private def validationError(field: String) = ValidationError(s"validation.businessContactDetails.$field.missing", field)

  implicit val errorCode: ErrorCode = "officerContactDetails.email"

  val form = Form(
    mapping(
      EMAIL -> textMapping()(s"$FORM_NAME.$EMAIL").verifying(regexPattern(EMAIL_PATTERN)(s"$FORM_NAME.$EMAIL")).verifying(maxLenText(EMAIL_MAX_LENGTH)),
      DAYTIME_PHONE -> optional(text.transform(removeSpaces, identity[String]).verifying(regexPattern(PHONE_NUMBER_PATTERN)(s"$FORM_NAME.$DAYTIME_PHONE"))),
      MOBILE -> optional(text.transform(removeSpaces, identity[String]).verifying(regexPattern(PHONE_NUMBER_PATTERN)(s"$FORM_NAME.$MOBILE"))),
      WEBSITE -> optional(text)
    )(BusinessContactDetails.apply)(BusinessContactDetails.unapply).verifying(atLeastOnePhoneNumber)
  )

  def atLeastOnePhoneNumber: Constraint[BusinessContactDetails] = Constraint {
    case BusinessContactDetails(_, None, None, _) =>
      Invalid(Seq(DAYTIME_PHONE, MOBILE).map(validationError))
    case _ => Valid
  }

}
