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

package forms.validation

import cats.Show
import forms.vatDetails.SortCode
import org.apache.commons.lang3.StringUtils
import play.api.data.format.Formatter
import play.api.data.validation._
import play.api.data.{FieldMapping, FormError, Mapping}

import scala.util.matching.Regex

private[forms] object FormValidation {

  def patternCheckingConstraint[T: Show](pattern: Regex, errorSubCode: String, mandatory: Boolean): Constraint[T] = Constraint {
    input: T =>
      val s: String = Show[T].show(input)
      mandatoryText(errorSubCode)(s) match {
        case Valid => Constraints.pattern(pattern, error = s"validation.$errorSubCode.invalid")(s)
        case err => if (mandatory) err else Valid
      }
  }

  def mandatoryText(specificCode: String): Constraint[String] = Constraint {
    (input: String) => if (StringUtils.isNotBlank(input)) Valid else Invalid(s"validation.$specificCode.missing")
  }

  val taxEstimateTextToLong = textToLong(0, 1000000000000000L) _
  def textToLong(min: Long, max: Long)(s: String): Long = {
    // assumes input string will be numeric
    val bigInt = BigInt(s)
    if (bigInt < min) {
      Long.MinValue
    } else if (bigInt > max) {
      Long.MaxValue
    } else {
      bigInt.toLong
    }
  }

  def longToText(l: Long): String = l.toString

  def boundedLong(specificCode: String): Constraint[Long] = Constraint {
    (input: Long) => if (input == Long.MaxValue) {
      Invalid(s"validation.$specificCode.high")
    } else if (input == Long.MinValue) {
      Invalid(s"validation.$specificCode.low")
    } else {
      Valid
    }
  }

  def nonEmptyValidText(specificCode: String, pattern: Regex): Constraint[String] = Constraint[String] {
    input: String =>
      input match {
        case `pattern`(_*) => Valid
        case s if StringUtils.isNotBlank(s) => Invalid(s"validation.$specificCode.invalid")
        case _ => Invalid(s"validation.$specificCode.empty")
      }
  }

  def stringFormat(specificCode: String): Formatter[String] = new Formatter[String] {
    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(Seq(FormError(key, s"validation.$specificCode.option.missing", Nil)))
    def unbind(key: String, value: String) = Map(key -> value)
  }

  def missingFieldMapping(specificCode: String): Mapping[String] = FieldMapping[String]()(stringFormat(specificCode))


  object BankAccount {

    import cats.instances.string._

    private val SortCode = """^([0-9]{2})-([0-9]{2})-([0-9]{2})$""".r
    private val AccountName = """[A-Za-z0-9\-',/& ]{1,150}""".r
    private val AccountNumber = """[0-9]{8}""".r


    def accountName(errorSubstring: String, mandatory: Boolean = true): Constraint[String] =
      patternCheckingConstraint(AccountName, s"$errorSubstring.name", mandatory)

    def accountNumber(errorSubstring: String, mandatory: Boolean = true): Constraint[String] =
      patternCheckingConstraint(AccountNumber, s"$errorSubstring.number", mandatory)

    def accountSortCode(errorSubstring: String, mandatory: Boolean = true): Constraint[SortCode] =
      patternCheckingConstraint(SortCode, errorSubCode = s"$errorSubstring.sortCode", mandatory)

  }

}
