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

package models.view.ppob

import models.api.{ScrsAddress, VatScheme}
import models.{ViewModelFormat, _}
import play.api.libs.json.Json

case class PpobView(addressId: String, address: Option[ScrsAddress] = None)

object PpobView {

  implicit val format = Json.format[PpobView]

  implicit val viewModelFormat = ViewModelFormat(
    readF = (group: S4LPpob) => group.address,
    updateF = (c: PpobView, g: Option[S4LPpob]) => g.getOrElse(S4LPpob()).copy(address = Some(c))
  )

  // return a view model from a VatScheme instance
  implicit val modelTransformer = ApiModelTransformer[PpobView] { vs: VatScheme =>
    vs.ppob.collect {
      case address  => PpobView(address.id, Some(address))
    }
  }

  // return a new or updated VatScheme from the PpobView instance
  implicit val viewModelTransformer = ViewModelTransformer { (c: PpobView, g: VatScheme) =>
    c.address.fold(g)(address => g.copy( ppob = Some(address)))
  }

}
