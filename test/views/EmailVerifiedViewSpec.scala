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

package views

import org.jsoup.Jsoup
import views.html.{email_verified, honesty_declaration}

class EmailVerifiedViewSpec extends VatRegViewSpec {

  val title = "Email address confirmed - Register for VAT - GOV.UK"
  val heading = "Email address confirmed"
  val buttonText = "Continue"

  "Email Verified Page" should {
    val view = app.injector.instanceOf[email_verified].apply(testCall)

    val doc = Jsoup.parse(view.body)

    "have the correct title" in {
      doc.title() mustBe title
    }

    "have the correct heading" in {
      doc.select(Selectors.h1).text() mustBe heading
    }

    "have the correct button" in {
      doc.select(Selectors.button).text() mustBe buttonText
    }

  }

}
