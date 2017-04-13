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

package testHelpers

import org.scalatest.compatible.Assertion
import org.scalatest.{Matchers, TestSuite}
import play.api.data.{Form, FormError}

trait FormInspectors extends Matchers {
  self: TestSuite =>

  val toErrorSeq = (fe: FormError) => (fe.key, fe.message)

  implicit class FormErrorOps(form: Form[_]) {

    def shouldHaveErrors(es: Seq[(String, String)]): Assertion = {
      form.errors shouldBe 'nonEmpty
      form.errors.size shouldBe es.size
      form.errors.map(toErrorSeq) shouldBe es
    }

  }

}
