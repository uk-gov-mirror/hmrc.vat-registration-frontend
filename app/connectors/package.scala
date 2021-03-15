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

import config.Logging
import play.api.http.Status
import uk.gov.hmrc.http.{BadRequestException, NotFoundException, UpstreamErrorResponse}

package object connectors extends Logging {
  def logResponse(e: Throwable, func: String): Throwable = {
    e match {
      case e: NotFoundException =>
        logger.warn(s"[$func] received NOT FOUND")
      case e: BadRequestException =>
        logger.warn(s"[$func] received BAD REQUEST")
      case UpstreamErrorResponse(message, status, _, _) => status match {
        case Status.FORBIDDEN =>
          logger.error(s"[$func] received FORBIDDEN")
        case s if s >= 400 && s <= 499 =>
          logger.error(s"[$func] received Upstream 4xx: ${status} ${message}")
        case s if s >= 500 && s <= 599 =>
          logger.error(s"[$func] received Upstream 5xx: ${status} ${message}")
      }
      case e: Exception =>
        logger.error(s"[$func] received ERROR: ${e.getMessage}")
    }
    e
  }
}
