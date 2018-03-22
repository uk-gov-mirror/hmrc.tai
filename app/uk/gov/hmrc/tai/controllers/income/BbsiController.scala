/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.controllers.income

import com.google.inject.{Inject, Singleton}
import play.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HttpException, NotFoundException}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.tai.model.api.{ApiFormats, ApiResponse}
import uk.gov.hmrc.tai.model.{AmountRequest, CloseAccountRequest}
import uk.gov.hmrc.tai.service.{BankAccountNotFound, BbsiService}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

@Singleton
class BbsiController @Inject()(bbsiService: BbsiService) extends BaseController with ApiFormats {

  def bbsiDetails(nino: Nino): Action[AnyContent] = Action.async { implicit request =>
    bbsiService.bbsiDetails(nino) map { accounts =>
      Ok(Json.toJson(ApiResponse(accounts, Nil)))
    }
  }

  def bbsiAccount(nino: Nino, id: Int): Action[AnyContent] = Action.async { implicit request =>
    bbsiService.bbsiAccount(nino, id).map {
      case Some(account) => Ok(Json.toJson(ApiResponse(account, Nil)))
      case None => NotFound
    }.recover {
      case _: NotFoundException => NotFound
      case _ => InternalServerError
    }
  }

  def closeBankAccount(nino: Nino, id: Int): Action[JsValue] = Action.async(parse.json) {
    implicit request => {
      withJsonBody[CloseAccountRequest] {
        closeAccountRequest =>
          bbsiService.closeBankAccount(nino, id, closeAccountRequest) map (envelopeId => {
            Ok(Json.toJson(ApiResponse(envelopeId, Nil)))
          })
      }
    } recoverWith {
      case BankAccountNotFound(body) =>
        Logger.warn(s"Bbsi - No bank account found for nino $nino")
        Future.successful(NotFound(body))
      case _ =>
        Future.successful(InternalServerError("Error while closing bank account"))
    }
  }

  def removeAccount(nino: Nino, id: Int): Action[AnyContent] = Action.async {
    implicit request => {
      bbsiService.removeIncorrectBankAccount(nino, id) map { envelopeId =>
        Accepted(Json.toJson(ApiResponse(envelopeId, Nil)))
      }
    } recoverWith {
      case BankAccountNotFound(body) =>
        Logger.warn(s"Bbsi - No bank account found for nino $nino")
        Future.successful(NotFound(body))
      case _ =>
        Future.successful(InternalServerError("Error while removing bank account"))
    }
  }

  def updateAccountInterest(nino: Nino, id: Int): Action[JsValue] = Action.async(parse.json) {
    implicit request => {
      withJsonBody[AmountRequest] {
        amountRequest =>
          bbsiService.updateBankAccountInterest(nino, id, amountRequest.amount) map (envelopeId => {
            Ok(Json.toJson(ApiResponse(envelopeId, Nil)))
          })
      }
    } recoverWith {
      case BankAccountNotFound(body) =>
        Logger.warn(s"Bbsi - No bank account found for nino $nino")
        Future.successful(NotFound(body))
      case ex: HttpException =>
        Future.failed(ex)
      case _ =>
        Future.successful(InternalServerError("Error while updating bank account"))
    }
  }

}