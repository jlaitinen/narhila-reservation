package api

import cats.effect.IO
import org.http4s.{AuthedRoutes, HttpRoutes, Response}
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
        for
          loginRequest <- req.as[LoginRequest]
          loginResponse <- userService.login(loginRequest)
          response <- Ok(loginResponse)
        yield response
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