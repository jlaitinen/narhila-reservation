package services

import shared.api.ApiRoutes
import shared.models._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Service for reservation-related operations
object ReservationService:
  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  // Get all reservations for the current user
  def getMyReservations: Future[List[ReservationView]] =
    AuthService.getToken match
      case Some(token) =>
        ApiService.get[List[ReservationView]](ApiRoutes.Reservations.BasePath, Some(token))
      case None =>
        Future.failed(new Exception("Not authenticated"))

  // Get a specific reservation
  def getReservation(id: String): Future[ReservationView] =
    AuthService.getToken match
      case Some(token) =>
        ApiService.get[ReservationView](ApiRoutes.Reservations.reservation(id), Some(token))
      case None =>
        Future.failed(new Exception("Not authenticated"))

  // Create a new reservation
  def createReservation(startDate: LocalDate, endDate: LocalDate): Future[ReservationView] =
    AuthService.getToken match
      case Some(token) =>
        val request = ReservationRequest(startDate, endDate)
        ApiService.post[ReservationRequest, ReservationView](
          ApiRoutes.Reservations.Create, 
          request, 
          Some(token)
        )
      case None =>
        Future.failed(new Exception("Not authenticated"))

  // Cancel a reservation
  def cancelReservation(id: String): Future[ReservationView] =
    AuthService.getToken match
      case Some(token) =>
        ApiService.delete[ReservationView](ApiRoutes.Reservations.reservation(id), Some(token))
      case None =>
        Future.failed(new Exception("Not authenticated"))

  // Get reservations for a specific period
  def getReservationsForPeriod(startDate: LocalDate, endDate: LocalDate): Future[List[ReservationView]] =
    AuthService.getToken match
      case Some(token) =>
        val startStr = startDate.format(dateFormatter)
        val endStr = endDate.format(dateFormatter)
        ApiService.get[List[ReservationView]](
          ApiRoutes.Reservations.byPeriod(startStr, endStr), 
          Some(token)
        )
      case None =>
        Future.failed(new Exception("Not authenticated"))