package service

import cats.effect.IO
import cats.implicits._
import repository.UserRepository
import model.{User, UserRole, UserRegistrationRequest, LoginRequest, LoginResponse, UserView}
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}
import io.circe.syntax._
import io.circe.Encoder.AsArray.importedAsArrayEncoder
import io.circe.Encoder.AsObject.importedAsObjectEncoder
import io.circe.Encoder.AsRoot.importedAsRootEncoder
import java.time.Instant

class UserService(userRepository: UserRepository, jwtSecret: String):
  def register(request: UserRegistrationRequest): IO[User] =
    for
      // Check if username already exists
      existingUsername <- userRepository.findByUsername(request.username)
      _ <- existingUsername match
        case Some(_) => IO.raiseError(new Exception("Username already exists"))
        case None => IO.unit
      
      // Check if email already exists
      existingEmail <- userRepository.findByEmail(request.email)
      _ <- existingEmail match
        case Some(_) => IO.raiseError(new Exception("Email already exists"))
        case None => IO.unit
      
      // Create and save user
      passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
      user = User(
        id = UUID.randomUUID(),
        username = request.username,
        email = request.email,
        passwordHash = passwordHash,
        role = UserRole.Regular
      )
      savedUser <- userRepository.create(user)
    yield savedUser

  def login(request: LoginRequest): IO[LoginResponse] =
    for
      user: User <- userRepository.findByUsername(request.username).flatMap {
        case Some(user) => IO.pure(user)
        case None => IO.raiseError(new Exception("Invalid username or password"))
      }
      
      _ <- IO.pure(BCrypt.checkpw(request.password, user.passwordHash)).flatMap {
        case true => IO.unit
        case false => IO.raiseError(new Exception("Invalid username or password"))
      }
      
      token = createToken(user)
      userView = UserView.fromUser(user)
    yield LoginResponse(token, userView)

  def validateToken(token: String): IO[UUID] =
    IO.fromEither(
      for {
        claim <- JwtCirce.decode(token, jwtSecret, Seq(JwtAlgorithm.HS256)).toEither
        contentMap <- io.circe.parser.decode[Map[String, String]](claim.content)
        userId <- contentMap.get("user_id").toRight(new Exception("No user ID in token"))
        uuid <- try {
          Right(UUID.fromString(userId))
        } catch {
          case _: Exception => Left(new Exception("Invalid user ID in token"))
        }
      } yield uuid
    ).handleErrorWith(_ => IO.raiseError(new Exception("Invalid token")))

  def getUserProfile(userId: UUID): IO[UserView] =
    userRepository.findById(userId).flatMap {
      case Some(user) => IO.pure(UserView.fromUser(user))
      case None => IO.raiseError(new Exception("User not found"))
    }

  private def createToken(user: User): String =
    val claim = JwtClaim(
      content = Map("user_id" -> user.id.toString).asJson.noSpaces,
      expiration = Some(Instant.now.plusSeconds(86400).getEpochSecond), // 24 hours
      issuedAt = Some(Instant.now.getEpochSecond)
    )
    JwtCirce.encode(claim, jwtSecret, JwtAlgorithm.HS256)