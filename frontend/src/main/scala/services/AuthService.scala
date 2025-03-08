package services

import shared.api.ApiRoutes
import shared.models._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import org.scalajs.dom.window.localStorage

// Service for authentication related operations
object AuthService:
  private val TokenKey = "auth_token"
  private val UserKey = "user_data"

  // Get the stored token
  def getToken: Option[String] = Option(localStorage.getItem(TokenKey))

  // Get the stored user
  def getUser: Option[UserView] = 
    Option(localStorage.getItem(UserKey)).flatMap { userData =>
      io.circe.parser.decode[UserView](userData).toOption
    }

  // Store authentication data
  def storeAuth(token: String, user: UserView): Unit =
    localStorage.setItem(TokenKey, token)
    localStorage.setItem(UserKey, io.circe.syntax.EncoderOps(user).asJson.noSpaces)

  // Clear authentication data
  def clearAuth(): Unit =
    localStorage.removeItem(TokenKey)
    localStorage.removeItem(UserKey)

  // Register a new user
  def register(username: String, email: String, password: String): Future[UserView] =
    val request = UserRegistrationRequest(username, email, password)
    ApiService.post[UserRegistrationRequest, UserView](ApiRoutes.Users.Register, request).map { user =>
      user
    }

  // Login user
  def login(username: String, password: String): Future[LoginResponse] =
    val request = LoginRequest(username, password)
    ApiService.post[LoginRequest, LoginResponse](ApiRoutes.Users.Login, request).map { response =>
      storeAuth(response.token, response.user)
      response
    }

  // Get current user profile
  def getCurrentUser: Future[UserView] =
    getToken match
      case Some(token) =>
        ApiService.get[UserView](ApiRoutes.Users.Profile, Some(token))
      case None =>
        Future.failed(new Exception("Not authenticated"))

  // Logout
  def logout(): Unit = clearAuth()