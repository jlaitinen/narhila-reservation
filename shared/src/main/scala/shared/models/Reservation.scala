package shared.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import java.time.{LocalDate, LocalDateTime}

enum ReservationStatus:
  case Pending, Confirmed, Cancelled

object ReservationStatus:
  given Encoder[ReservationStatus] = Encoder.encodeString.contramap {
    case ReservationStatus.Pending => "Pending"
    case ReservationStatus.Confirmed => "Confirmed"
    case ReservationStatus.Cancelled => "Cancelled"
  }

  given Decoder[ReservationStatus] = Decoder.decodeString.map {
    case "Pending" => ReservationStatus.Pending
    case "Confirmed" => ReservationStatus.Confirmed
    case "Cancelled" => ReservationStatus.Cancelled
    case _ => ReservationStatus.Pending // Default to Pending for unknown values
  }

// Request to create a reservation
final case class ReservationRequest(
  startDate: LocalDate,
  endDate: LocalDate
)

object ReservationRequest:
  given Encoder[ReservationRequest] = deriveEncoder
  given Decoder[ReservationRequest] = deriveDecoder

// Reservation data that is sent to the client
final case class ReservationView(
  id: String,
  userId: String,
  startDate: LocalDate,
  endDate: LocalDate,
  createdAt: LocalDateTime,
  status: ReservationStatus
)

object ReservationView:
  given Encoder[ReservationView] = deriveEncoder
  given Decoder[ReservationView] = deriveDecoder