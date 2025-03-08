package model

import java.time.LocalDate
import java.util.UUID
import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.{deriveEncoder, deriveDecoder}
import doobie.util.{Read, Write}
import doobie.util.meta.Meta

// User model
case class User(
  id: UUID,
  username: String,
  email: String,
  passwordHash: String,
  role: UserRole
)

object User:
  given Encoder[User] = deriveEncoder[User]
  given Decoder[User] = deriveDecoder[User]

enum UserRole:
  case Regular, Admin

object UserRole:
  given Encoder[UserRole] = Encoder.encodeString.contramap {
    case UserRole.Regular => "regular"
    case UserRole.Admin => "admin"
  }
  
  given Decoder[UserRole] = Decoder.decodeString.map {
    case "regular" => UserRole.Regular
    case "admin" => UserRole.Admin
    case other => throw new IllegalArgumentException(s"Unknown role: $other")
  }

// Reservation model
case class Reservation(
  id: UUID,
  userId: UUID,
  startDate: LocalDate,
  endDate: LocalDate,
  createdAt: java.time.Instant,
  status: ReservationStatus
)

object Reservation:
  given Encoder[Reservation] = deriveEncoder[Reservation]
  given Decoder[Reservation] = deriveDecoder[Reservation]

enum ReservationStatus:
  case Pending, Confirmed, Cancelled

object ReservationStatus:
  given Encoder[ReservationStatus] = Encoder.encodeString.contramap {
    case ReservationStatus.Pending => "pending"
    case ReservationStatus.Confirmed => "confirmed"
    case ReservationStatus.Cancelled => "cancelled"
  }
  
  given Decoder[ReservationStatus] = Decoder.decodeString.map {
    case "pending" => ReservationStatus.Pending
    case "confirmed" => ReservationStatus.Confirmed
    case "cancelled" => ReservationStatus.Cancelled
    case other => throw new IllegalArgumentException(s"Unknown status: $other")
  }

// DTOs for requests/responses
case class UserRegistrationRequest(
  username: String,
  email: String,
  password: String
)

object UserRegistrationRequest:
  given Encoder[UserRegistrationRequest] = deriveEncoder[UserRegistrationRequest]
  given Decoder[UserRegistrationRequest] = deriveDecoder[UserRegistrationRequest]

case class LoginRequest(
  username: String,
  password: String
)

object LoginRequest:
  given Encoder[LoginRequest] = deriveEncoder[LoginRequest]
  given Decoder[LoginRequest] = deriveDecoder[LoginRequest]

case class LoginResponse(
  token: String,
  user: UserView
)

object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder[LoginResponse]
  given Decoder[LoginResponse] = deriveDecoder[LoginResponse]

case class UserView(
  id: UUID,
  username: String,
  email: String,
  role: UserRole
)

object UserView:
  def fromUser(user: User): UserView = 
    UserView(user.id, user.username, user.email, user.role)
  
  given Encoder[UserView] = deriveEncoder[UserView]
  given Decoder[UserView] = deriveDecoder[UserView]

case class ReservationRequest(
  startDate: LocalDate,
  endDate: LocalDate
)

object ReservationRequest:
  given Encoder[ReservationRequest] = deriveEncoder[ReservationRequest]
  given Decoder[ReservationRequest] = deriveDecoder[ReservationRequest]

case class ReservationView(
  id: UUID,
  startDate: LocalDate,
  endDate: LocalDate,
  status: ReservationStatus,
  createdAt: java.time.Instant
)

object ReservationView:
  def fromReservation(reservation: Reservation): ReservationView =
    ReservationView(
      reservation.id,
      reservation.startDate,
      reservation.endDate,
      reservation.status,
      reservation.createdAt
    )
  
  given Encoder[ReservationView] = deriveEncoder[ReservationView]
  given Decoder[ReservationView] = deriveDecoder[ReservationView]