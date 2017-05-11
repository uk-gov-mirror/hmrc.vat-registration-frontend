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

package models.api

import cats.Show
import models.ApiModelTransformer
import models.api.ScrsAddress.inlineShow.inline
import models.view.vatLodgingOfficer.OfficerHomeAddressView
import org.apache.commons.lang3.text.WordUtils
import play.api.libs.json.{Json, OFormat}

case class ScrsAddress(
                        line1: String,
                        line2: String,
                        line3: Option[String] = None,
                        line4: Option[String] = None,
                        postcode: Option[String] = None,
                        country: Option[String] = None
                      ) {

  val id: String = Seq(Some(line1), if (postcode.isDefined) postcode else country).flatten.mkString.replaceAll(" ", "")

  val asLabel: String = inline show this

  override def equals(obj: Any): Boolean = obj match {
    case ScrsAddress(objLine1, _, _, _, Some(objPostcode), _)
      if objPostcode != "" => line1.equalsIgnoreCase(objLine1) && postcode.getOrElse("").equalsIgnoreCase(objPostcode)
    case ScrsAddress(objLine1, _, _, _, None, Some(objCountry))
      if objCountry != "" => line1.equalsIgnoreCase(objLine1) && country.getOrElse("").equalsIgnoreCase(objCountry)

    case _ => false
  }

  override def hashCode: Int = asLabel.##
}

object ScrsAddress {

  private sealed trait AddressLineOrPostcode

  private final case class AddressLine(line: String) extends AddressLineOrPostcode

  private final case class Postcode(postcode: String) extends AddressLineOrPostcode

  implicit val format: OFormat[ScrsAddress] = Json.format[ScrsAddress]

  private def normalisedSeq(address: ScrsAddress): Seq[String] = {
    import cats.instances.option._
    import cats.syntax.applicative._

    Seq[Option[AddressLineOrPostcode]](
      address.line1.pure.map(AddressLine),
      address.line2.pure.map(AddressLine),
      address.line3.map(AddressLine),
      address.line4.map(AddressLine),
      address.postcode.map(Postcode),
      address.country.map(AddressLine)
    ).collect {
      case Some(AddressLine(line)) => WordUtils.capitalizeFully(line)
      case Some(Postcode(postcode)) => postcode.toUpperCase()
    }
  }

  object htmlShow {
    implicit val html = Show.show { a: ScrsAddress =>
      normalisedSeq(a).mkString("<br />")
    }
  }

  object inlineShow {
    implicit val inline = Show.show { a: ScrsAddress =>
      normalisedSeq(a).mkString(", ")
    }
  }

  implicit def modelTransformer(implicit transformer: ApiModelTransformer[OfficerHomeAddressView]): ApiModelTransformer[ScrsAddress] =
    ApiModelTransformer { vatScheme: VatScheme =>
      transformer.toViewModel(vatScheme).flatMap(_.address)
    }

}
