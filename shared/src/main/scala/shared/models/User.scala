package shared.models

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

enum UserRole:
  case User, Admin

object UserRole:
  given Encoder[UserRole] = Encoder.encodeString.contramap {
    case UserRole.User => "User"
    case UserRole.Admin => "Admin"
  }

  given Decoder[UserRole] = Decoder.decodeString.map {
    case "User" => UserRole.User 
    case "Admin" => UserRole.Admin
    case _ => UserRole.User // Default to User for unknown values
  }

// Model for user registration
final case class UserRegistrationRequest(
  username: String,
  email: String,
  password: String
)

object UserRegistrationRequest:
  given Encoder[UserRegistrationRequest] = deriveEncoder
  given Decoder[UserRegistrationRequest] = deriveDecoder

// Model for login
final case class LoginRequest(
  username: String,
  password: String
)

object LoginRequest:
  given Encoder[LoginRequest] = deriveEncoder
  given Decoder[LoginRequest] = deriveDecoder

// Response after login
final case class LoginResponse(
  token: String,
  user: UserView
)

object LoginResponse:
  given Encoder[LoginResponse] = deriveEncoder
  given Decoder[LoginResponse] = deriveDecoder

// User data that is safe to send to the client
final case class UserView(
  id: String,
  username: String,
  email: String,
  role: UserRole
)

object UserView:
  given Encoder[UserView] = deriveEncoder
  given Decoder[UserView] = deriveDecoder