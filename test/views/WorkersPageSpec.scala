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

import forms.WorkersForm
import org.jsoup.Jsoup
import views.html.labour.workers

class WorkersPageSpec extends VatRegViewSpec {

  lazy val view = app.injector.instanceOf[workers]
  implicit val doc = Jsoup.parse(view(WorkersForm.form).body)

  object ExpectedContent {
    val title = "How many supply workers do you supply? - Register for VAT - GOV.UK"
    val heading = "How many supply workers do you supply?"
    val button = "Continue"
  }

  "Workers Page" should {
    "have the correct page title" in new ViewSetup {
      doc.title mustBe ExpectedContent.title
    }
    "have the correct heading" in new ViewSetup {
      doc.heading mustBe Some(ExpectedContent.heading)
    }
    "have a label that's the same as the heading" in new ViewSetup {
      doc.textBox("numberOfWorkers") mustBe Some(ExpectedContent.heading)
    }
    "have a primary action" in new ViewSetup {
      doc.submitButton mustBe Some(ExpectedContent.button)
    }
  }

}
