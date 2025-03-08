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
      // Validate input parameters
      _ <- IO.raiseWhen(request.username == null || request.username.trim.isEmpty)(
        new Exception("Username cannot be empty")
      )
      _ <- IO.raiseWhen(request.password == null)(
        new Exception("Password cannot be empty")
      )
      
      // Find user by username
      user: User <- userRepository.findByUsername(request.username).flatMap {
        case Some(user) => IO.pure(user)
        case None => IO.raiseError(new Exception("Invalid username or password"))
      }
      
      // Verify password hash is not null
      _ <- IO.raiseWhen(user.passwordHash == null)(
        new Exception("User has invalid password hash")
      )
      
      // Check password
      _ <- IO.pure(BCrypt.checkpw(request.password, user.passwordHash)).flatMap {
        case true => IO.unit
        case false => IO.raiseError(new Exception("Invalid username or password"))
      }
      
      // Create token and response
      token = createToken(user)
      userView = UserView.fromUser(user)
    yield LoginResponse(token, userView)

  def validateToken(token: String): IO[UUID] =
    // Check for null or empty token
    if (token == null || token.trim.isEmpty) {
      IO.raiseError(new Exception("Token cannot be empty"))
    } else {
      IO.fromEither(
        for {
          // Decode JWT
          claim <- JwtCirce.decode(token, jwtSecret, Seq(JwtAlgorithm.HS256)).toEither
          
          // Parse content
          content = if (claim.content == null) "" else claim.content
          contentMap <- io.circe.parser.decode[Map[String, String]](content)
            .left.map(_ => new Exception("Invalid token content"))
          
          // Extract user ID
          userId <- contentMap.get("user_id")
            .toRight(new Exception("No user ID in token"))
          
          // Parse UUID
          uuid <- try {
            Right(UUID.fromString(userId))
          } catch {
            case _: Exception => Left(new Exception("Invalid user ID format in token"))
          }
        } yield uuid
      ).handleErrorWith(e => 
        // Log detailed error but return generic message to user
        IO(System.err.println(s"Token validation error: ${e.getMessage}")) *>
        IO.raiseError(new Exception("Invalid or expired token"))
      )
    }

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