package cottageservices.config

import cats.effect.{IO, Resource}
import doobie.hikari.HikariTransactor
import org.flywaydb.core.Flyway
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Database:
  def transactor(dbConfig: DbConfig): Resource[IO, HikariTransactor[IO]] =
    // ExecutionContext for transactions
    val connectEC = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(dbConfig.maxConnections))
    
    for
      xa <- HikariTransactor.newHikariTransactor[IO](
        driverClassName = dbConfig.driver,
        url = dbConfig.url,
        user = dbConfig.user,
        pass = dbConfig.password,
        connectEC
      )
    yield xa

  def initialize(transactor: HikariTransactor[IO]): IO[Unit] =
    transactor.configure { dataSource =>
      IO {
        val flyway = Flyway.configure()
          .dataSource(dataSource)
          .load()
        
        flyway.migrate()
      }
    }

case class DbConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  maxConnections: Int = 10,
  migrationsTable: String = "schema_migrations",
  migrationsLocations: List[String] = List("classpath:db/migration")
)