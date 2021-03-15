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
import models.TransactorDetails
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SoleTraderIdentificationConnector @Inject()(httpClient: HttpClient)
                                                 (implicit executionContext: ExecutionContext,
                                                  appConfig: FrontendAppConfig) {

  private val journeyIdKey = "journeyId"

  private def loggingPrefix = s"[${getClass.getSimpleName}][${getClass.getEnclosingMethod.getName}]"

  def createJourney(implicit hc: HeaderCarrier): Future[String] = {
    httpClient.POST(appConfig.getSoleTraderIdentificationJourneyUrl, Json.obj()) map { response =>
      response.status match {
        case CREATED => (response.json \  journeyIdKey).validate[String] match {
          case JsSuccess(journeyId, _) => journeyId
        }
        case UNAUTHORIZED =>
          throw new InternalServerException(s"$loggingPrefix Failed to create new journey as user was unauthorised")
        case status =>
          throw new InternalServerException(s"$loggingPrefix Sole trader identification returned an unexpected status: $status")
      }
    }
  }


  def retrieveSoleTraderDetails(journeyId: String)(implicit hc: HeaderCarrier): Future[TransactorDetails] =
    httpClient.GET[JsValue](appConfig.getRetrieveSoleTraderIdentificationResultUrl(journeyId)) map { response =>
      response.validate[TransactorDetails] match {
        case JsSuccess(transactorDetails, _) =>
          transactorDetails
        case JsError(errors) =>
          throw new InternalServerException(s"Sole trader ID returned invalid JSON ${errors.map(_._1).mkString(", ")}")
      }
    }

}
