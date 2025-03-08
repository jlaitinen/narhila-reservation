package components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import services.AuthService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

// Material UI components
@js.native
@JSImport("@mui/material", "TextField")
object TextField extends js.Object

@js.native
@JSImport("@mui/material", "Button")
object Button extends js.Object

@js.native
@JSImport("@mui/material", "Box")
object Box extends js.Object

@js.native
@JSImport("@mui/material", "Typography")
object Typography extends js.Object

@js.native
@JSImport("@mui/material", "Alert")
object Alert extends js.Object

case class LoginFormProps(
  onLoginSuccess: () => Callback
)

case class LoginFormState(
  username: String = "",
  password: String = "",
  errorMessage: Option[String] = None,
  isSubmitting: Boolean = false
)

object LoginForm:
  class Backend($: BackendScope[LoginFormProps, LoginFormState]):
    def render(props: LoginFormProps, state: LoginFormState) =
      <.div(
        ^.className := "login-form",
        ^.style := js.Dynamic.literal(
          maxWidth = "400px",
          margin = "0 auto",
          padding = "2rem"
        ),
        <.h1(
          ^.textAlign := "center",
          ^.marginBottom := "1rem",
          "Login"
        ),
        
        state.errorMessage.map { error =>
          <.div(
            ^.backgroundColor := "#f44336",
            ^.color := "white",
            ^.padding := "10px",
            ^.marginBottom := "1rem",
            ^.borderRadius := "4px",
            error
          )
        }.getOrElse(EmptyVdom),
        
        <.form(
          ^.onSubmit ==> { e => 
            e.preventDefault()
            handleSubmit(props)
          },
          
          <.div(
            ^.marginBottom := "1rem",
            <.label(
              ^.display := "block",
              ^.marginBottom := "0.5rem",
              "Username"
            ),
            <.input(
              ^.`type` := "text",
              ^.value := state.username,
              ^.onChange ==> { e => 
                val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                $.modState(_.copy(username = value))
              },
              ^.disabled := state.isSubmitting,
              ^.width := "100%",
              ^.padding := "8px",
              ^.boxSizing := "border-box",
              ^.border := "1px solid #ccc",
              ^.borderRadius := "4px"
            )
          ),
          
          <.div(
            ^.marginBottom := "1rem",
            <.label(
              ^.display := "block",
              ^.marginBottom := "0.5rem",
              "Password"
            ),
            <.input(
              ^.`type` := "password",
              ^.value := state.password,
              ^.onChange ==> { e => 
                val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                $.modState(_.copy(password = value))
              },
              ^.disabled := state.isSubmitting,
              ^.width := "100%",
              ^.padding := "8px",
              ^.boxSizing := "border-box",
              ^.border := "1px solid #ccc",
              ^.borderRadius := "4px"
            )
          ),
          
          <.button(
            ^.`type` := "submit",
            ^.disabled := state.isSubmitting,
            ^.width := "100%",
            ^.padding := "10px",
            ^.marginTop := "1rem",
            ^.backgroundColor := "#1976d2",
            ^.color := "white",
            ^.border := "none",
            ^.borderRadius := "4px",
            ^.cursor := "pointer",
            if (state.isSubmitting) "Logging in..." else "Login"
          )
        )
      )

    def handleSubmit(props: LoginFormProps): Callback = 
      $.modState(_.copy(isSubmitting = true, errorMessage = None)) >>
      Callback.future {
        val state = $.state.runNow()
        AuthService.login(state.username, state.password).map { _ =>
          $.modState(_.copy(isSubmitting = false)) >> props.onLoginSuccess()
        }.recover {
          case e: Exception =>
            $.modState(_.copy(
              isSubmitting = false,
              errorMessage = Some(s"Login failed: ${e.getMessage}")
            ))
        }
      }

  val component = ScalaComponent.builder[LoginFormProps]
    .initialState(LoginFormState())
    .renderBackend[Backend]
    .build