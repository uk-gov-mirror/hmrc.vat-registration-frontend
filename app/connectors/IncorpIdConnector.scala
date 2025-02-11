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

package connectors

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import models.external.incorporatedentityid.{IncorpIdJourneyConfig, IncorporationDetails}
import play.api.http.Status.CREATED
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, InternalServerException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits.{readRaw, readFromJson}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncorpIdConnector @Inject()(httpClient: HttpClient, config: FrontendAppConfig)(implicit ec: ExecutionContext) {

  def createJourney(journeyConfig: IncorpIdJourneyConfig)(implicit hc: HeaderCarrier): Future[String] = {
    val url = config.getCreateIncorpIdJourneyUrl()

    httpClient.POST(url, journeyConfig).map {
      case response@HttpResponse(CREATED, _, _) =>
        (response.json \ "journeyStartUrl").as[String]
      case response =>
        throw new InternalServerException(s"Invalid response from incorporated entity identification: Status: ${response.status} Body: ${response.body}")
    }
  }

  def getDetails(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporationDetails] = {
    val url = config.getIncorpIdDetailsUrl(journeyId)

    httpClient.GET[JsValue](url)
      .map(json => {
        IncorporationDetails.apiFormat.reads(json) match {
          case JsSuccess(value, _) => value
          case JsError(errors) => throw new Exception(s"Incorp ID returned invalid JSON ${errors.map(_._1).mkString(", ")}")
        }
      })
  }

}
