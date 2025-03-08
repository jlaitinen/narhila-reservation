package cottageservices

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.Router
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cottageservices.config.{AppConfig, Database}
import repository.{DoobieUserRepository, DoobieReservationRepository}
import service.{UserService, ReservationService}
import api.{UserRoutes, ReservationRoutes, AuthMiddleware}
import com.comcast.ip4s.{ipv4, port}

object Server extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    for
      logger <- Slf4jLogger.create[IO]
      _ <- logger.info("Starting Cottage Reservation Service")
      
      // Load configuration
      config <- AppConfig.load()
      _ <- logger.info(s"Loaded configuration for ${config.name}")
      
      // Initialize database
      _ <- Database.transactor(config.database).use { xa => 
        for
          _ <- logger.info("Initializing database")
          _ <- Database.initialize(xa)
          
          // Create repositories
          userRepo = new DoobieUserRepository(xa)
          reservationRepo = new DoobieReservationRepository(xa)
          
          // Create services
          userService = new UserService(userRepo, config.security.jwtSecret)
          reservationService = new ReservationService(reservationRepo)
          
          // Create authentication middleware
          authMiddleware = AuthMiddleware.authUser(userService)
          
          // Define HTTP routes
          httpApp = Router(
            "/" -> UserRoutes.routes(userService),
            "/" -> ReservationRoutes.routes(reservationService, authMiddleware)
          ).orNotFound
          
          // Add request logging
          loggedApp = Logger.httpApp(logHeaders = true, logBody = true)(httpApp)
          
          // Start HTTP server
          _ <- logger.info(s"Starting HTTP server at ${config.http.host}:${config.http.port}")
          _ <- EmberServerBuilder.default[IO]
            .withHost(ipv4"0.0.0.0")
            .withPort(port"8081") // Changed to 8081 to avoid conflict with frontend
            .withHttpApp(loggedApp)
            .build
            .use(_ => IO.never)
            .handleErrorWith { error =>
              logger.error(error)("Server error") *> IO.pure(ExitCode.Error)
            }
        yield ()
      }
    yield ExitCode.Success