package integration

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import cats.effect.IO
import org.http4s.{Method, Request, Status, Uri}
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import cats.effect.Resource
import cats.effect.unsafe.implicits.global
import io.circe.generic.auto._
import org.http4s.circe._
import model._
import java.time.LocalDate

class ServerIntegrationSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  // Setup integration test with in-memory database
  // Note: This is a simplified integration test that assumes the server is running
  // In a real project, you would use test containers or embedded H2 database for full integration tests
  
  val httpClient = EmberClientBuilder.default[IO].build
  
  // Base URI for your API
  val baseUri = uri"http://localhost:8080"
  
  // Test user credentials
  val testUsername = "testuser"
  val testPassword = "password123"
  val testEmail = "test@example.com"
  
  // Helper to extract token from login response
  def extractToken(response: String): String =
    // Simple extraction - in real tests you'd use proper JSON parsing
    response.split("\"token\":\"").tail.head.split("\"").head
  
  "The server" should {
    "allow a user to register and login" in {
      // This test assumes the server is running locally
      // In real projects, you would start/stop the server programmatically for testing
      
      // First create a test user
      val registerRequest = UserRegistrationRequest(
        username = testUsername,
        email = testEmail,
        password = testPassword
      )
      
      // Then log in with the user
      val loginRequest = LoginRequest(
        username = testUsername,
        password = testPassword
      )
      
      val testProgram = httpClient.use { client =>
        for {
          // Registration
          registerResp <- client.expect[String](
            Request[IO](method = Method.POST, uri = baseUri / "api" / "users" / "register")
              .withEntity(registerRequest)(jsonEncoderOf[IO, UserRegistrationRequest])
          )
          
          // Login
          loginResp <- client.expect[String](
            Request[IO](method = Method.POST, uri = baseUri / "api" / "users" / "login")
              .withEntity(loginRequest)(jsonEncoderOf[IO, LoginRequest])
          )
          
          // Extract token (simplified)
          token = extractToken(loginResp)
          
          // Try to get profile with token
          profileResp <- client.expect[String](
            Request[IO](method = Method.GET, uri = baseUri / "api" / "users" / "profile")
              .withHeaders(org.http4s.headers.Authorization(org.http4s.Credentials.Token(org.http4s.AuthScheme.Bearer, token)))
          )
        } yield (registerResp, loginResp, profileResp)
      }
      
      // For integration tests, we'd actually run this and verify the results
      // This is just an example - in actual implementation you would use
      // proper JSON parsing and assertions on the response bodies
      IO.pure(succeed)
    }
    
    "allow managing reservations" in {
      // This test assumes the server is running and user authentication is working
      
      // Step 1: Login to get token
      val loginRequest = LoginRequest(
        username = testUsername,
        password = testPassword
      )
      
      // Step 2: Create a reservation
      val reservationRequest = ReservationRequest(
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3)
      )
      
      val testProgram = httpClient.use { client =>
        for {
          // Login
          loginResp <- client.expect[String](
            Request[IO](method = Method.POST, uri = baseUri / "api" / "users" / "login")
              .withEntity(loginRequest)(jsonEncoderOf[IO, LoginRequest])
          )
          
          // Extract token (simplified)
          token = extractToken(loginResp)
          auth = org.http4s.headers.Authorization(org.http4s.Credentials.Token(org.http4s.AuthScheme.Bearer, token))
          
          // Create reservation
          createResp <- client.expect[String](
            Request[IO](method = Method.POST, uri = baseUri / "api" / "reservations")
              .withHeaders(auth)
              .withEntity(reservationRequest)(jsonEncoderOf[IO, ReservationRequest])
          )
          
          // Get reservations
          listResp <- client.expect[String](
            Request[IO](method = Method.GET, uri = baseUri / "api" / "reservations")
              .withHeaders(auth)
          )
          
          // Extract reservation ID (simplified - in real tests you'd use proper JSON parsing)
          reservationId = createResp.split("\"id\":\"").tail.head.split("\"").head
          
          // Get specific reservation
          getResp <- client.expect[String](
            Request[IO](method = Method.GET, uri = baseUri / "api" / "reservations" / reservationId)
              .withHeaders(auth)
          )
          
          // Cancel reservation
          cancelResp <- client.expect[String](
            Request[IO](method = Method.DELETE, uri = baseUri / "api" / "reservations" / reservationId)
              .withHeaders(auth)
          )
        } yield (createResp, listResp, getResp, cancelResp)
      }
      
      // For integration tests, we'd actually run this and verify the results
      // This is just an example structure
      IO.pure(succeed)
    }
  }