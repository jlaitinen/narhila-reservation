package api

import cats.effect.IO
import org.http4s.{AuthedRoutes, HttpRoutes, Response, Status}
import org.http4s.dsl.io.*
import org.http4s.circe.CirceEntityCodec.*
import service.UserService
import model.{LoginRequest, UserRegistrationRequest, UserView}
import io.circe.generic.auto.*
import org.http4s.server.Router
import org.http4s.AuthedRoutes
import api.AuthMiddleware.AuthedUser

object UserRoutes:
  def routes(userService: UserService): HttpRoutes[IO] =
    val publicRoutes = HttpRoutes.of[IO] {
      case req @ POST -> Root / "register" =>
        for
          registerRequest <- req.as[UserRegistrationRequest]
          user <- userService.register(registerRequest)
          response <- Created(UserView.fromUser(user))
        yield response

      case req @ POST -> Root / "login" =>
        req.as[LoginRequest].flatMap { loginRequest =>
          // Add better error handling
          userService.login(loginRequest).attempt.flatMap {
            case Right(loginResponse) => 
              Ok(loginResponse)
            case Left(error) => 
              // Log the full error but return a simplified message to the client
              // Create a mock LoginResponse on auth error to match expected format
              IO(System.err.println(s"Login error: ${error.getMessage}")) *>
              // Return a proper 401 response and let the client handle it
              IO.pure(Response[IO](status = Status.Unauthorized).withEntity(
                Map("error" -> "Invalid username or password")
              ))
          }
        }
    }

    val authMiddleware = AuthMiddleware.authUser(userService)
    val authedRoutes = authMiddleware(AuthedRoutes.of[AuthedUser, IO] {
      case GET -> Root / "profile" as authedUser =>
        userService.getUserProfile(authedUser.id).flatMap(profile => Ok(profile))
    })

    Router(
      "/api/users" -> authedRoutes,
      "/api/users" -> publicRoutes
    )