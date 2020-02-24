/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.tai.config

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait HodConfig {
  val baseURL: String
  val environment: String
  val authorization: String
  val originatorId: String
}

abstract class BaseConfig(playEnv: Environment) {
  val mode = playEnv.mode
}

@Singleton
class PdfConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment, config: ServicesConfig)
    extends BaseConfig(playEnv) {
  lazy val baseURL: String = config.baseUrl("pdf-generator-service")
}

@Singleton
class PayeConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment, config: ServicesConfig)
    extends BaseConfig(playEnv) {
  lazy val baseURL: String = config.baseUrl("paye")
}

@Singleton
class FileUploadConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment, config: ServicesConfig)
    extends BaseConfig(playEnv) {
  lazy val baseURL: String = config.baseUrl("file-upload")
  lazy val frontendBaseURL: String = config.baseUrl("file-upload-frontend")
  lazy val callbackUrl: String = config.getConfString("file-upload.callbackUrl", "")
  lazy val intervalMs: Int = runModeConfiguration.getOptional[Int]("file-upload.intervalMs").getOrElse(20)
  lazy val maxAttempts: Int = runModeConfiguration.getOptional[Int]("file-upload.maxAttempts").getOrElse(5)
}

@Singleton
class CitizenDetailsConfig @Inject()(
  val runModeConfiguration: Configuration,
  playEnv: Environment,
  config: ServicesConfig)
    extends BaseConfig(playEnv) {
  lazy val baseURL: String = config.baseUrl("citizen-details")
}

@Singleton
class DesConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment, config: ServicesConfig)
    extends BaseConfig(playEnv) with HodConfig {
  lazy val baseURL: String = config.baseUrl("des-hod")
  lazy val environment: String = config.getConfString("des-hod.env", "local")
  lazy val authorization: String = "Bearer " + config.getConfString("des-hod.authorizationToken", "local")
  lazy val daPtaOriginatorId: String = config.getConfString("des-hod.da-pta.originatorId", "")
  lazy val originatorId: String = config.getConfString("des-hod.originatorId", "")
}

@Singleton
class NpsConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment, config: ServicesConfig)
    extends BaseConfig(playEnv) with HodConfig {
  private lazy val path: String = config.getConfString("nps-hod.path", "")

  override lazy val baseURL: String = s"${config.baseUrl("nps-hod")}$path"
  override lazy val environment = ""
  override lazy val authorization = ""
  override lazy val originatorId: String = config.getConfString("nps-hod.originatorId", "local")
  lazy val autoUpdatePayEnabled: Option[Boolean] = runModeConfiguration.getOptional[Boolean]("auto-update-pay.enabled")
  lazy val updateSourceEnabled: Option[Boolean] = runModeConfiguration.getOptional[Boolean]("nps-update-source.enabled")
  lazy val postCalcEnabled: Option[Boolean] = runModeConfiguration.getOptional[Boolean]("nps-post-calc.enabled")
}

@Singleton
class CyPlusOneConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment)
    extends BaseConfig(playEnv) {
  lazy val cyPlusOneEnabled: Option[Boolean] = runModeConfiguration.getOptional[Boolean]("cy-plus-one.enabled")
  lazy val cyPlusOneEnableDate: Option[String] = runModeConfiguration.getOptional[String]("cy-plus-one.startDayMonth")
}

@Singleton
class MongoConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment) extends BaseConfig(playEnv) {
  lazy val mongoEnabled: Boolean = runModeConfiguration.getOptional[Boolean]("cache.isEnabled").getOrElse(false)
  lazy val mongoEncryptionEnabled: Boolean =
    runModeConfiguration.getOptional[Boolean]("mongo.encryption.enabled").getOrElse(true)
}

@Singleton
class FeatureTogglesConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment)
    extends BaseConfig(playEnv) {
  def desEnabled: Boolean = runModeConfiguration.getOptional[Boolean]("tai.des.call").getOrElse(false)
  def desUpdateEnabled: Boolean = runModeConfiguration.getOptional[Boolean]("tai.des.update.call").getOrElse(false)
  def confirmedAPIEnabled: Boolean =
    runModeConfiguration.getOptional[Boolean]("tai.confirmedAPI.enabled").getOrElse(false)
}

@Singleton
class CacheMetricsConfig @Inject()(val runModeConfiguration: Configuration, playEnv: Environment)
    extends BaseConfig(playEnv) {
  def cacheMetricsEnabled: Boolean =
    runModeConfiguration.getOptional[Boolean]("tai.cacheMetrics.enabled").getOrElse(false)
}
