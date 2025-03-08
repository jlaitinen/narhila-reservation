package service

import cats.effect.IO
import cats.implicits._
import repository.ReservationRepository
import model.{Reservation, ReservationRequest, ReservationView, ReservationStatus}
import java.time.{LocalDate, Instant}
import java.util.UUID

class ReservationService(reservationRepository: ReservationRepository):
  def createReservation(userId: UUID, request: ReservationRequest): IO[ReservationView] =
    for
      // Validate dates
      _ <- validateDates(request.startDate, request.endDate)
      
      // Check for overlapping reservations
      overlapping <- reservationRepository.findOverlapping(request.startDate, request.endDate)
      _ <- if (overlapping.nonEmpty) IO.raiseError(new Exception("The requested dates overlap with existing reservations"))
           else IO.unit
      
      // Create reservation
      reservation = Reservation(
        id = UUID.randomUUID(),
        userId = userId,
        startDate = request.startDate,
        endDate = request.endDate,
        createdAt = Instant.now(),
        status = ReservationStatus.Pending
      )
      
      saved <- reservationRepository.create(reservation)
    yield ReservationView.fromReservation(saved)

  def getUserReservations(userId: UUID): IO[List[ReservationView]] =
    reservationRepository.findByUserId(userId).map { reservations =>
      reservations.map(ReservationView.fromReservation)
    }

  def getReservation(id: UUID): IO[ReservationView] =
    reservationRepository.findById(id).flatMap {
      case Some(reservation) => IO.pure(ReservationView.fromReservation(reservation))
      case None => IO.raiseError(new Exception("Reservation not found"))
    }

  def cancelReservation(id: UUID, userId: UUID): IO[ReservationView] =
    for
      reservation <- reservationRepository.findById(id).flatMap {
        case Some(r) => IO.pure(r)
        case None => IO.raiseError(new Exception("Reservation not found"))
      }
      
      // Check if the reservation belongs to the user
      _ <- if (reservation.userId != userId) IO.raiseError(new Exception("Not authorized to cancel this reservation"))
           else IO.unit
      
      // Check if the reservation is already cancelled
      _ <- if (reservation.status == ReservationStatus.Cancelled) IO.raiseError(new Exception("Reservation is already cancelled"))
           else IO.unit
      
      updated <- reservationRepository.updateStatus(id, ReservationStatus.Cancelled).flatMap {
        case Some(r) => IO.pure(ReservationView.fromReservation(r))
        case None => IO.raiseError(new Exception("Failed to cancel reservation"))
      }
    yield updated

  def confirmReservation(id: UUID): IO[ReservationView] =
    reservationRepository.findById(id).flatMap {
      case Some(reservation) if reservation.status == ReservationStatus.Pending => 
        reservationRepository.updateStatus(id, ReservationStatus.Confirmed).flatMap {
          case Some(r) => IO.pure(ReservationView.fromReservation(r))
          case None => IO.raiseError(new Exception("Failed to confirm reservation"))
        }
      case Some(_) => IO.raiseError(new Exception("Reservation cannot be confirmed (wrong status)"))
      case None => IO.raiseError(new Exception("Reservation not found"))
    }

  def getReservationsForPeriod(startDate: LocalDate, endDate: LocalDate): IO[List[ReservationView]] =
    for
      _ <- validateDates(startDate, endDate)
      reservations <- reservationRepository.findAllBetweenDates(startDate, endDate)
    yield reservations.map(ReservationView.fromReservation)

  private def validateDates(startDate: LocalDate, endDate: LocalDate): IO[Unit] =
    if (startDate.isAfter(endDate))
      IO.raiseError(new Exception("Start date must be before end date"))
    else if (startDate.isBefore(LocalDate.now()))
      IO.raiseError(new Exception("Start date cannot be in the past"))
    else
      IO.unit