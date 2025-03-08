package pages

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js
import components._
import services.AuthService

// Enum to represent different pages
sealed trait Page
object Page:
  case object Login extends Page
  case object Register extends Page
  case object Calendar extends Page

case class AppState(
  currentPage: Page = Page.Login,
  isInitializing: Boolean = true
)

object App:
  class Backend($: BackendScope[Unit, AppState]):
    def render(state: AppState) =
      <.div(
        // App content
        <.div(
          ^.style := js.Dynamic.literal(
            margin = "0 auto",
            padding = "1rem",
            fontFamily = "Roboto, sans-serif"
          ),
          
          if (state.isInitializing) {
            <.div(
              ^.style := js.Dynamic.literal(
                textAlign = "center",
                padding = "4rem"
              ),
              "Loading..."
            )
          } else {
            renderCurrentPage(state)
          }
        )
      )
    
    // Render the current page based on state
    private def renderCurrentPage(state: AppState) =
      state.currentPage match
        case Page.Login =>
          LoginForm.component(LoginFormProps(onLoginSuccess = () => navigateTo(Page.Calendar)))
        
        case Page.Register =>
          RegisterForm.component(RegisterFormProps(
            onRegisterSuccess = () => navigateTo(Page.Login),
            onBackToLogin = () => navigateTo(Page.Login)
          ))
        
        case Page.Calendar =>
          ReservationCalendar.component(ReservationCalendarProps(
            onLogout = () => handleLogout
          ))
    
    // Navigate to a different page
    def navigateTo(page: Page): Callback = $.modState(_.copy(currentPage = page))
    
    // Handle user logout
    def handleLogout: Callback = 
      Callback {
        AuthService.logout()
      } >> navigateTo(Page.Login)
    
    // Check authentication status when component mounts
    val didMount: Callback = Callback {
      AuthService.getToken match
        case Some(_) =>
          // User is already logged in, go to calendar
          $.modState(_.copy(currentPage = Page.Calendar, isInitializing = false)).runNow()
        case None =>
          // User is not logged in, stay on login page
          $.modState(_.copy(isInitializing = false)).runNow()
    }
  
  val component = ScalaComponent.builder[Unit]
    .initialState(AppState())
    .renderBackend[Backend]
    .componentDidMount(_.backend.didMount)
    .build