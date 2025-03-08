package api

import cats.effect.IO
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io.*
import org.http4s.circe.CirceEntityCodec.*
import service.ReservationService
import model.{ReservationRequest, ReservationView}
import io.circe.generic.auto.*

import java.util.UUID
import java.time.LocalDate
import org.http4s.server.Router
import org.http4s.AuthedRoutes
import api.AuthMiddleware.AuthedUser
import org.http4s.server.AuthMiddleware as Http4sAuthMiddleware

object ReservationRoutes:
  def routes(reservationService: ReservationService, authMiddleware: Http4sAuthMiddleware[IO, AuthedUser]): HttpRoutes[IO] =
    val authedRoutes = authMiddleware(AuthedRoutes.of[AuthedUser, IO] {
      // Get all reservations for the logged-in user
      case GET -> Root as authedUser =>
        for
          reservations <- reservationService.getUserReservations(authedUser.id)
          response <- Ok(reservations)
        yield response

      // Get a specific reservation
      case GET -> Root / UUIDVar(id) as authedUser =>
        reservationService.getReservation(id).flatMap(Ok(_))
          .handleErrorWith {
            case e: Exception => NotFound(e.getMessage)
          }

      // Create a new reservation
      case req @ POST -> Root as authedUser =>
        import org.http4s.circe.CirceEntityCodec._
        for
          reservationRequest <- req.req.as[ReservationRequest]
          reservation <- reservationService.createReservation(authedUser.id, reservationRequest)
          response <- Created(reservation)
        yield response

      // Cancel a reservation
      case DELETE -> Root / UUIDVar(id) as authedUser =>
        reservationService.cancelReservation(id, authedUser.id).flatMap(Ok(_))
          .handleErrorWith {
            case e: Exception => BadRequest(e.getMessage)
          }

      // Get reservations for a specific period
      case GET -> Root / "period" :? StartDateParam(startDate) +& EndDateParam(endDate) as _ =>
        val start = parseDate(startDate, LocalDate.now())
        val end = parseDate(endDate, LocalDate.now().plusMonths(1))
        
        reservationService.getReservationsForPeriod(start, end).flatMap(Ok(_))
          .handleErrorWith {
            case e: Exception => BadRequest(e.getMessage)
          }
    })

    Router("/api/reservations" -> authedRoutes)

  // Query parameter extractors
  object StartDateParam extends OptionalQueryParamDecoderMatcher[String]("startDate")
  object EndDateParam extends OptionalQueryParamDecoderMatcher[String]("endDate")

  private def parseDate(dateStr: Option[String], defaultDate: LocalDate): LocalDate =
    dateStr.map(LocalDate.parse).getOrElse(defaultDate)