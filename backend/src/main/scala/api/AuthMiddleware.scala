package api

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware => Http4sAuthMiddleware}
import service.UserService
import java.util.UUID

object AuthMiddleware:
  case class AuthedUser(id: UUID)

  def authUser(userService: UserService): Http4sAuthMiddleware[IO, AuthedUser] =
    val authUser = Kleisli { (request: Request[IO]) =>
      import org.http4s.syntax.all._
      val authHeader: Option[String] = request.headers.get[Authorization].flatMap { auth =>
        val credStr = auth.credentials.renderString
        if (credStr != null && credStr.startsWith("Bearer ")) {
          Some(credStr.replace("Bearer ", ""))
        } else {
          None
        }
      }

      OptionT(authHeader match {
        case Some(token) =>
          userService.validateToken(token)
            .map(userId => Some(AuthedUser(userId)))
            .handleError(_ => None)
        case None => IO.pure(None)
      })
    }

    Http4sAuthMiddleware(authUser)