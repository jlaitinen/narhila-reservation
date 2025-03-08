package components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import services.AuthService
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

case class RegisterFormProps(
  onRegisterSuccess: () => Callback,
  onBackToLogin: () => Callback
)

case class RegisterFormState(
  username: String = "",
  email: String = "",
  password: String = "",
  confirmPassword: String = "",
  errorMessage: Option[String] = None,
  isSubmitting: Boolean = false
)

object RegisterForm:
  class Backend($: BackendScope[RegisterFormProps, RegisterFormState]):
    def render(props: RegisterFormProps, state: RegisterFormState) =
      <.div(
        ^.className := "register-form",
        ^.style := js.Dynamic.literal(
          maxWidth = "400px",
          margin = "0 auto",
          padding = "2rem"
        ),
        <.h1(
          ^.textAlign := "center",
          ^.marginBottom := "1rem",
          "Create an Account"
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
              "Email"
            ),
            <.input(
              ^.`type` := "email",
              ^.value := state.email,
              ^.onChange ==> { e => 
                val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                $.modState(_.copy(email = value))
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
          
          <.div(
            ^.marginBottom := "1rem",
            <.label(
              ^.display := "block",
              ^.marginBottom := "0.5rem",
              "Confirm Password"
            ),
            <.input(
              ^.`type` := "password",
              ^.value := state.confirmPassword,
              ^.onChange ==> { e => 
                val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                $.modState(_.copy(confirmPassword = value))
              },
              ^.disabled := state.isSubmitting,
              ^.width := "100%",
              ^.padding := "8px",
              ^.boxSizing := "border-box",
              ^.border := (if (state.password != state.confirmPassword && state.confirmPassword.nonEmpty) 
                           "1px solid #f44336" 
                         else 
                           "1px solid #ccc"),
              ^.borderRadius := "4px"
            ),
            (if (state.password != state.confirmPassword && state.confirmPassword.nonEmpty) 
              <.div(
                ^.color := "#f44336",
                ^.fontSize := "12px",
                ^.marginTop := "4px",
                "Passwords do not match"
              ) 
            else 
              EmptyVdom)
          ),
          
          <.button(
            ^.`type` := "submit",
            ^.disabled := state.isSubmitting || state.password != state.confirmPassword,
            ^.width := "100%",
            ^.padding := "10px",
            ^.marginTop := "1rem",
            ^.backgroundColor := "#1976d2",
            ^.color := "white",
            ^.border := "none",
            ^.borderRadius := "4px",
            ^.cursor := "pointer",
            if (state.isSubmitting) "Registering..." else "Register"
          ),
          
          <.button(
            ^.`type` := "button",
            ^.onClick --> props.onBackToLogin(),
            ^.disabled := state.isSubmitting,
            ^.width := "100%",
            ^.padding := "10px",
            ^.marginTop := "0.5rem",
            ^.backgroundColor := "transparent",
            ^.color := "#1976d2",
            ^.border := "none",
            ^.cursor := "pointer",
            "Back to Login"
          )
        )
      )

    def handleSubmit(props: RegisterFormProps): Callback = 
      val state = $.state.runNow()
      
      if (state.password != state.confirmPassword) {
        $.modState(_.copy(errorMessage = Some("Passwords do not match")))
      } else {
        $.modState(_.copy(isSubmitting = true, errorMessage = None)) >>
        Callback.future {
          AuthService.register(state.username, state.email, state.password).map { _ =>
            $.modState(_.copy(isSubmitting = false)) >> props.onRegisterSuccess()
          }.recover {
            case e: Exception =>
              $.modState(_.copy(
                isSubmitting = false,
                errorMessage = Some(s"Registration failed: ${e.getMessage}")
              ))
          }
        }
      }

  val component = ScalaComponent.builder[RegisterFormProps]
    .initialState(RegisterFormState())
    .renderBackend[Backend]
    .build