package api

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.generic.auto._
import org.http4s.circe._
import java.util.UUID
import java.time.Instant
import model._
import service.UserService
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar

class UserRoutesSpec extends AnyWordSpec with Matchers with MockitoSugar:

  // Json codecs for HTTP
  implicit val userDecoder: EntityDecoder[IO, User] = jsonOf[IO, User]
  implicit val userViewDecoder: EntityDecoder[IO, UserView] = jsonOf[IO, UserView]
  implicit val loginResponseDecoder: EntityDecoder[IO, LoginResponse] = jsonOf[IO, LoginResponse]
  implicit val registerReqEncoder: EntityEncoder[IO, UserRegistrationRequest] = jsonEncoderOf[IO, UserRegistrationRequest]
  implicit val loginReqEncoder: EntityEncoder[IO, LoginRequest] = jsonEncoderOf[IO, LoginRequest]

  "UserRoutes" should {
    "allow user registration" in {
      // Arrange
      val mockUserService = mock[UserService]
      val routes = UserRoutes.routes(mockUserService)
      
      val testUser = User(
        id = UUID.randomUUID(),
        username = "testuser",
        email = "test@example.com",
        passwordHash = "hashed_password",
        role = UserRole.Regular
      )
      
      val registerRequest = UserRegistrationRequest(
        username = "testuser",
        email = "test@example.com",
        password = "password123"
      )
      
      when(mockUserService.register(any[UserRegistrationRequest])).thenReturn(IO.pure(testUser))
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.POST, uri = uri"/api/users/register")
          .withEntity(registerRequest)
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Created
      val userView = response.as[UserView].unsafeRunSync()
      userView.username shouldBe testUser.username
      userView.email shouldBe testUser.email
    }
    
    "allow user login" in {
      // Arrange
      val mockUserService = mock[UserService]
      val routes = UserRoutes.routes(mockUserService)
      
      val testUser = User(
        id = UUID.randomUUID(),
        username = "testuser",
        email = "test@example.com",
        passwordHash = "hashed_password",
        role = UserRole.Regular
      )
      
      val loginRequest = LoginRequest(
        username = "testuser",
        password = "password123"
      )
      
      val loginResponse = LoginResponse(
        token = "fake-jwt-token",
        user = UserView.fromUser(testUser)
      )
      
      when(mockUserService.login(any[LoginRequest])).thenReturn(IO.pure(loginResponse))
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.POST, uri = uri"/api/users/login")
          .withEntity(loginRequest)
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val resp = response.as[LoginResponse].unsafeRunSync()
      resp.token shouldBe "fake-jwt-token"
      resp.user.username shouldBe testUser.username
    }
    
    "fetch user profile" in {
      // Arrange
      val mockUserService = mock[UserService]
      val routes = UserRoutes.routes(mockUserService)
      
      val userId = UUID.randomUUID()
      val testUser = User(
        id = userId,
        username = "testuser",
        email = "test@example.com",
        passwordHash = "hashed_password",
        role = UserRole.Regular
      )
      
      val userView = UserView.fromUser(testUser)
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockUserService.getUserProfile(any[UUID])).thenReturn(IO.pure(userView))
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.GET, uri = uri"/api/users/profile")
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val profile = response.as[UserView].unsafeRunSync()
      profile.username shouldBe testUser.username
      profile.email shouldBe testUser.email
    }
  }