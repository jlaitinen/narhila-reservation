package cottageservices.config

import cats.effect.IO
import pureconfig._
import pureconfig.generic.derivation.default._
import pureconfig.ConfigSource

case class AppConfig(
  name: String,
  http: HttpConfig,
  security: SecurityConfig,
  database: DbConfig
) derives ConfigReader

case class HttpConfig(
  host: String,
  port: Int
) derives ConfigReader

case class SecurityConfig(
  jwtSecret: String
) derives ConfigReader

object AppConfig:
  def load(): IO[AppConfig] =
    IO {
      ConfigSource.default.at("app").loadOrThrow[AppConfig]
    }