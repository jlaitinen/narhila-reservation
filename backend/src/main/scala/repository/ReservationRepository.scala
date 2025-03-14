package repository

import cats.effect.IO
import doobie._
import doobie.implicits._
import doobie.util.meta.Meta
import doobie.implicits.javasql._
import doobie.postgres.implicits._
import doobie.h2._
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import model.{Reservation, ReservationStatus}
import java.time.{LocalDate, Instant}
import java.util.UUID

trait ReservationRepository:
  def create(reservation: Reservation): IO[Reservation]
  def findById(id: UUID): IO[Option[Reservation]]
  def findByUserId(userId: UUID): IO[List[Reservation]]
  def findAllBetweenDates(startDate: LocalDate, endDate: LocalDate): IO[List[Reservation]]
  def findOverlapping(startDate: LocalDate, endDate: LocalDate): IO[List[Reservation]]
  def updateStatus(id: UUID, status: ReservationStatus): IO[Option[Reservation]]
  def delete(id: UUID): IO[Boolean]

class DoobieReservationRepository(xa: Transactor[IO]) extends ReservationRepository:
  import ReservationRepository._
  private def reservationFromRow(id: UUID, userId: UUID, startDate: LocalDate, endDate: LocalDate, createdAt: Instant, status: ReservationStatus): Reservation =
    Reservation(id, userId, startDate, endDate, createdAt, status)

  def create(reservation: Reservation): IO[Reservation] = {
    val sql = fr"""
      INSERT INTO reservations (id, user_id, start_date, end_date, created_at, status)
      VALUES (${reservation.id}, ${reservation.userId}, ${reservation.startDate}, 
              ${reservation.endDate}, ${reservation.createdAt}, ${reservation.status.toString})
    """
    sql.update.run.transact(xa).as(reservation)
  }

  def findById(id: UUID): IO[Option[Reservation]] = {
    val sql = fr"""
      SELECT id, user_id, start_date, end_date, created_at, status
      FROM reservations
      WHERE id = ${id.toString}
    """
    sql.query[(String, String, LocalDate, LocalDate, Instant, String)].map { case (id, userid, start, end, created, status) =>
      try {
        val stat = ReservationStatus.valueOf(status)
        reservationFromRow(UUID.fromString(id), UUID.fromString(userid), start, end, created, stat)
      } catch {
        case e: Exception => {
          IO(System.err.println(s"Invalid UUID format: $id, error: ${e.getMessage}"))
          Reservation(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.now(), Instant.now(), ReservationStatus.Pending)
        }
      }

    }.option.transact(xa)
  }

  def findByUserId(userId: UUID): IO[List[Reservation]] = {
    val sql = fr"""
      SELECT id, user_id, start_date, end_date, created_at, status
      FROM reservations
      WHERE user_id = ${userId.toString}
    """
    sql.query[(String, String, LocalDate, LocalDate, Instant, String)].map{
      case (id, uid, start, end, created, status) =>
        try {
          val stat = ReservationStatus.valueOf(status)
          reservationFromRow(UUID.fromString(id), UUID.fromString(uid), start, end, created, stat)
        } catch {
          case e: Exception => {
            IO(System.err.println(s"Invalid UUID format: $id, error: ${e.getMessage}"))
            Reservation(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.now(), Instant.now(), ReservationStatus.Pending)
          }
        }}
      .to[List].transact(xa)
  }

  def findAllBetweenDates(startDate: LocalDate, endDate: LocalDate): IO[List[Reservation]] = {
    val sql = fr"""
      SELECT id, user_id, start_date, end_date, created_at, status
      FROM reservations
      WHERE (start_date BETWEEN $startDate AND $endDate)
         OR (end_date BETWEEN $startDate AND $endDate)
         OR (start_date <= $startDate AND end_date >= $endDate)
    """
    sql.query[(String, String, LocalDate, LocalDate, Instant, String)].map {
        case (id, uid, start, end, created, status) =>
          try {
            val stat = ReservationStatus.valueOf(status)
            reservationFromRow(UUID.fromString(id), UUID.fromString(uid), start, end, created, stat)
          } catch {
            case e: Exception => {
              IO(System.err.println(s"Invalid UUID format: $id, error: ${e.getMessage}"))
              Reservation(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.now(), Instant.now(), ReservationStatus.Pending)
            }
          }
      }
      .to[List].transact(xa)
  }

  def findOverlapping(startDate: LocalDate, endDate: LocalDate): IO[List[Reservation]] = {
    val sql = fr"""
      SELECT id, user_id, start_date, end_date, created_at, status
      FROM reservations
      WHERE status != 'Cancelled'
        AND ((start_date BETWEEN $startDate AND $endDate)
         OR (end_date BETWEEN $startDate AND $endDate)
         OR (start_date <= $startDate AND end_date >= $endDate))
    """
    sql.query[(String, String, LocalDate, LocalDate, Instant, String)].map {
        case (id, uid, start, end, created, status) =>
          try {
            val stat = ReservationStatus.valueOf(status)
            reservationFromRow(UUID.fromString(id), UUID.fromString(uid), start, end, created, stat)
          } catch {
            case e: Exception => {
              IO(System.err.println(s"Invalid UUID format: $id, error: ${e.getMessage}"))
              Reservation(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), LocalDate.now(), Instant.now(), ReservationStatus.Pending)
            }
          }
      }
      .to[List].transact(xa)
  }

  def updateStatus(id: UUID, status: ReservationStatus): IO[Option[Reservation]] = {
    val updateSql = fr"""
      UPDATE reservations
      SET status = ${status.toString}
      WHERE id = ${id.toString}
    """
    for {
      _ <- updateSql.update.run.transact(xa)
      updated <- findById(id)
    } yield updated
  }

  def delete(id: UUID): IO[Boolean] = {
    val sql = fr"DELETE FROM reservations WHERE id = ${id.toString}"
    sql.update.run.transact(xa).map(_ > 0)
  }

object ReservationRepository:
  // Meta instance for UUID
  given Meta[UUID] = Meta[String].imap(UUID.fromString)(_.toString)
  
  // Meta instance for LocalDate
  given Meta[LocalDate] = Meta[java.sql.Date].imap(_.toLocalDate)(java.sql.Date.valueOf)
  
  // Meta instance for Instant
  given Meta[Instant] = Meta[java.sql.Timestamp].imap(_.toInstant)(timestamp => 
    java.sql.Timestamp.from(timestamp)
  )
  
  // Meta instance for ReservationStatus
  given Meta[ReservationStatus] = Meta[String].imap {
    case "Pending" => ReservationStatus.Pending
    case "Confirmed" => ReservationStatus.Confirmed
    case "Cancelled" => ReservationStatus.Cancelled
    case status => throw new IllegalArgumentException(s"Unknown status: $status")
  }(_.toString)