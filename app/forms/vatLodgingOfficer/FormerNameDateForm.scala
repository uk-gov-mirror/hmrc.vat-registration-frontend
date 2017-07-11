/*
 * Copyright 2017 HM Revenue & Customs
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

package forms.vatLodgingOfficer

import java.time.LocalDate

import forms.FormValidation.Dates.{nonEmptyDateModel, validDateModel}
import forms.FormValidation._
import models.DateModel
import models.view.vatLodgingOfficer.FormerNameDateView
import play.api.data.Form
import play.api.data.Forms.{mapping, text}

object FormerNameDateForm {

  implicit val errorCode: ErrorCode = "formerNameDate"

  implicit object LocalDateOrdering extends Ordering[LocalDate] {
    override def compare(x: LocalDate, y: LocalDate): Int = x.compareTo(y)
  }

  val minDate: LocalDate = LocalDate.of(1900, 1, 1)
  val maxDate: LocalDate = LocalDate.now()

  val form = Form(
    mapping (
      "date" -> mapping(
        "day" -> text,
        "month" -> text,
        "year" -> text
      )(DateModel.apply)(DateModel.unapply).verifying(nonEmptyDateModel(validDateModel(inRange(minDate, maxDate))))
    ) (FormerNameDateView.bind)(FormerNameDateView.unbind)
  )
}
