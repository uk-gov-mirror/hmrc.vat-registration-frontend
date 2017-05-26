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

package models.view.vatLodgingOfficer

import models._
import models.api.{VatLodgingOfficer, VatScheme}
import models.external.Officer
import play.api.libs.json.Json

case class CompletionCapacityView(id: String, officer: Option[Officer] = None)

object CompletionCapacityView {

  def apply(o: Officer): CompletionCapacityView = new CompletionCapacityView(o.name.id, Some(o))

  implicit val format = Json.format[CompletionCapacityView]

  implicit val vmReads = VMReads(
    readF = (group: S4LVatLodgingOfficer) => group.completionCapacity,
    updateF = (c: CompletionCapacityView, g: Option[S4LVatLodgingOfficer]) =>
      g.getOrElse(S4LVatLodgingOfficer()).copy(completionCapacity = Some(c))
  )

  // return a view model from a VatScheme instance
  implicit val modelTransformer = ApiModelTransformer[CompletionCapacityView] { vs: VatScheme =>
    vs.lodgingOfficer.map(o => CompletionCapacityView(o.name.id, Some(Officer(o.name, o.role, None, None))))
  }

  // return a new or updated VatLodgingOfficer from the CompleteCapacityView instance
  implicit val viewModelTransformer = ViewModelTransformer { (c: CompletionCapacityView, g: VatLodgingOfficer) =>
    g.copy(name = c.officer.getOrElse(Officer.empty).name, role = c.officer.getOrElse(Officer.empty).role)
  }
}
