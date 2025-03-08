package components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}
import scala.scalajs.js.{Date => JSDate, Object => JsObject, Array => JsArray, |, UndefOr}
import scala.concurrent.ExecutionContext.Implicits.global
import services.ReservationService
import shared.models.{ReservationView, ReservationStatus}
import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

case class ReservationCalendarProps(
  onLogout: () => Callback
)

case class ReservationCalendarState(
  reservations: List[ReservationView] = List.empty,
  isLoading: Boolean = true,
  error: Option[String] = None,
  selectedStartDate: Option[LocalDate] = None,
  selectedEndDate: Option[LocalDate] = None,
  isDialogOpen: Boolean = false,
  isSubmitting: Boolean = false,
  dialogError: Option[String] = None,
  snackbarOpen: Boolean = false,
  snackbarMessage: String = ""
)

object ReservationCalendar:
  class Backend($: BackendScope[ReservationCalendarProps, ReservationCalendarState]):
    def render(props: ReservationCalendarProps, state: ReservationCalendarState) =
      <.div(
        ^.className := "reservation-calendar-container",
        ^.style := js.Dynamic.literal(
          padding = "2rem",
          maxWidth = "1000px",
          margin = "0 auto"
        ),
        
        // Header
        <.div(
          ^.style := js.Dynamic.literal(
            display = "flex",
            justifyContent = "space-between",
            alignItems = "center",
            marginBottom = "2rem"
          ),
          <.h1(
            ^.style := js.Dynamic.literal(
              margin = "0"
            ),
            "Reservation Calendar"
          ),
          <.button(
            ^.onClick --> props.onLogout(),
            ^.style := js.Dynamic.literal(
              padding = "8px 16px",
              backgroundColor = "transparent",
              border = "1px solid #1976d2",
              color = "#1976d2",
              borderRadius = "4px",
              cursor = "pointer"
            ),
            "Logout"
          )
        ),
        
        // Error message
        state.error.map { error =>
          <.div(
            ^.style := js.Dynamic.literal(
              backgroundColor = "#f44336",
              color = "white",
              padding = "10px",
              marginBottom = "1rem",
              borderRadius = "4px"
            ),
            error
          )
        }.getOrElse(EmptyVdom),
        
        // Loading indicator
        if (state.isLoading) {
          <.div(
            ^.style := js.Dynamic.literal(
              textAlign = "center",
              padding = "2rem"
            ),
            "Loading reservations..."
          )
        } else {
          // Calendar container
          <.div(
            ^.style := js.Dynamic.literal(
              padding = "1.5rem",
              marginBottom = "2rem",
              border = "1px solid #ddd",
              borderRadius = "4px",
              backgroundColor = "white"
            ),
            
            // Instructions
            <.p(
              ^.style := js.Dynamic.literal(
                marginBottom = "1rem"
              ),
              "Select a date range to make a new reservation."
            ),
            
            // Legend
            <.div(
              ^.style := js.Dynamic.literal(
                display = "flex",
                gap = "1rem",
                marginBottom = "1rem"
              ),
              legendItem("Pending", "reservation-pending"),
              legendItem("Confirmed", "reservation-confirmed"),
              legendItem("Cancelled", "reservation-cancelled"),
              legendItem("Your Selection", "react-calendar__tile--active")
            ),
            
            // Simple date selector
            <.div(
              ^.style := js.Dynamic.literal(
                display = "flex",
                gap = "1rem",
                marginBottom = "1rem"
              ),
              <.div(
                ^.style := js.Dynamic.literal(
                  flex = "1"
                ),
                <.label(
                  ^.display := "block",
                  ^.marginBottom := "0.5rem",
                  "Start Date"
                ),
                <.input(
                  ^.`type` := "date",
                  ^.value := state.selectedStartDate.map(_.toString).getOrElse(""),
                  ^.onChange ==> { e => 
                    val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                    val date = if (value.isEmpty) None else Some(LocalDate.parse(value))
                    $.modState(_.copy(selectedStartDate = date))
                  },
                  ^.min := LocalDate.now().toString,
                  ^.style := js.Dynamic.literal(
                    width = "100%",
                    padding = "8px",
                    boxSizing = "border-box"
                  )
                )
              ),
              <.div(
                ^.style := js.Dynamic.literal(
                  flex = "1"
                ),
                <.label(
                  ^.display := "block",
                  ^.marginBottom := "0.5rem",
                  "End Date"
                ),
                <.input(
                  ^.`type` := "date",
                  ^.value := state.selectedEndDate.map(_.toString).getOrElse(""),
                  ^.onChange ==> { e => 
                    val value = e.target.asInstanceOf[org.scalajs.dom.HTMLInputElement].value
                    val date = if (value.isEmpty) None else Some(LocalDate.parse(value))
                    $.modState(_.copy(selectedEndDate = date))
                  },
                  ^.min := state.selectedStartDate.map(_.toString).getOrElse(LocalDate.now().toString),
                  ^.style := js.Dynamic.literal(
                    width = "100%",
                    padding = "8px",
                    boxSizing = "border-box"
                  )
                )
              )
            ),
            
            // Create reservation button
            <.button(
              ^.onClick --> handleOpenDialog,
              ^.disabled := state.selectedStartDate.isEmpty || state.selectedEndDate.isEmpty,
              ^.style := js.Dynamic.literal(
                padding = "10px",
                backgroundColor = "#1976d2",
                color = "white",
                border = "none",
                borderRadius = "4px",
                cursor = "pointer",
                width = "100%"
              ),
              "Create Reservation"
            ),
            
            // Display reservations
            <.div(
              ^.style := js.Dynamic.literal(
                marginTop = "2rem"
              ),
              <.h3("Your Reservations"),
              if (state.reservations.isEmpty) {
                <.p("You don't have any reservations yet.")
              } else {
                <.table(
                  ^.style := js.Dynamic.literal(
                    width = "100%",
                    borderCollapse = "collapse"
                  ),
                  <.thead(
                    <.tr(
                      <.th(^.style := js.Dynamic.literal(textAlign = "left", padding = "8px", borderBottom = "1px solid #ddd"), "Start Date"),
                      <.th(^.style := js.Dynamic.literal(textAlign = "left", padding = "8px", borderBottom = "1px solid #ddd"), "End Date"),
                      <.th(^.style := js.Dynamic.literal(textAlign = "left", padding = "8px", borderBottom = "1px solid #ddd"), "Status")
                    )
                  ),
                  <.tbody(
                    state.reservations.map { reservation =>
                      <.tr(
                        ^.key := reservation.id,
                        <.td(^.style := js.Dynamic.literal(padding = "8px", borderBottom = "1px solid #ddd"), 
                             formatLocalDate(reservation.startDate)),
                        <.td(^.style := js.Dynamic.literal(padding = "8px", borderBottom = "1px solid #ddd"), 
                             formatLocalDate(reservation.endDate)),
                        <.td(^.style := js.Dynamic.literal(padding = "8px", borderBottom = "1px solid #ddd"), 
                             <.span(
                               ^.className := s"reservation-${reservation.status.toString.toLowerCase}",
                               ^.style := js.Dynamic.literal(
                                 padding = "4px 8px",
                                 borderRadius = "4px",
                                 display = "inline-block"
                               ),
                               reservation.status.toString
                             ))
                      )
                    }.toTagMod
                  )
                )
              }
            )
          )
        },
        
        // Dialog for confirming reservation
        if (state.isDialogOpen) {
          <.div(
            ^.style := js.Dynamic.literal(
              position = "fixed",
              top = "0",
              left = "0",
              right = "0",
              bottom = "0",
              backgroundColor = "rgba(0,0,0,0.5)",
              display = "flex",
              alignItems = "center",
              justifyContent = "center",
              zIndex = "1000"
            ),
            <.div(
              ^.style := js.Dynamic.literal(
                backgroundColor = "white",
                padding = "1.5rem",
                borderRadius = "4px",
                width = "400px",
                maxWidth = "90%"
              ),
              <.h2("Confirm Reservation"),
              
              // Error message
              state.dialogError.map { error =>
                <.div(
                  ^.style := js.Dynamic.literal(
                    backgroundColor = "#f44336",
                    color = "white",
                    padding = "10px",
                    marginBottom = "1rem",
                    borderRadius = "4px"
                  ),
                  error
                )
              }.getOrElse(EmptyVdom),
              
              // Date range info
              (state.selectedStartDate, state.selectedEndDate) match {
                case (Some(start), Some(end)) =>
                  <.p(
                    s"Are you sure you want to make a reservation from ${formatLocalDate(start)} to ${formatLocalDate(end)}?"
                  )
                case _ => 
                  EmptyVdom
              },
              
              <.div(
                ^.style := js.Dynamic.literal(
                  display = "flex",
                  justifyContent = "flex-end",
                  gap = "0.5rem",
                  marginTop = "1.5rem"
                ),
                <.button(
                  ^.onClick --> $.modState(_.copy(isDialogOpen = false)),
                  ^.disabled := state.isSubmitting,
                  ^.style := js.Dynamic.literal(
                    padding = "8px 16px",
                    backgroundColor = "transparent",
                    border = "1px solid #ddd",
                    borderRadius = "4px",
                    cursor = "pointer"
                  ),
                  "Cancel"
                ),
                <.button(
                  ^.onClick --> handleCreateReservation,
                  ^.disabled := state.isSubmitting,
                  ^.style := js.Dynamic.literal(
                    padding = "8px 16px",
                    backgroundColor = "#1976d2",
                    color = "white",
                    border = "none",
                    borderRadius = "4px",
                    cursor = "pointer"
                  ),
                  if (state.isSubmitting) "Creating..." else "Create Reservation"
                )
              )
            )
          )
        } else {
          EmptyVdom
        },
        
        // Snackbar
        if (state.snackbarOpen) {
          <.div(
            ^.style := js.Dynamic.literal(
              position = "fixed",
              bottom = "20px",
              left = "50%",
              transform = "translateX(-50%)",
              backgroundColor = "#333",
              color = "white",
              padding = "10px 20px",
              borderRadius = "4px",
              boxShadow = "0 2px 10px rgba(0,0,0,0.2)",
              zIndex = "1000",
              display = "flex",
              alignItems = "center",
              justifyContent = "space-between",
              minWidth = "300px"
            ),
            <.span(state.snackbarMessage),
            <.button(
              ^.onClick --> $.modState(_.copy(snackbarOpen = false)),
              ^.style := js.Dynamic.literal(
                backgroundColor = "transparent",
                border = "none",
                color = "white",
                cursor = "pointer",
                fontSize = "20px",
                marginLeft = "10px"
              ),
              "×"
            )
          )
        } else {
          EmptyVdom
        }
      )
      
    // Helper to render legend items
    private def legendItem(label: String, className: String) =
      <.div(
        ^.style := js.Dynamic.literal(
          display = "flex",
          alignItems = "center",
          gap = "0.5rem"
        ),
        <.div(
          ^.className := className,
          ^.style := js.Dynamic.literal(
            width = "20px",
            height = "20px",
            borderRadius = "4px"
          )
        ),
        <.span(label)
      )
    
    // Format LocalDate
    private def formatLocalDate(date: LocalDate): String =
      val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
      date.format(formatter)
    
    // Open dialog to confirm reservation
    def handleOpenDialog: Callback =
      $.state.flatMap { state =>
        (state.selectedStartDate, state.selectedEndDate) match {
          case (Some(start), Some(end)) =>
            $.modState(_.copy(
              isDialogOpen = true,
              dialogError = None
            ))
          case _ =>
            Callback.empty
        }
      }
    
    // Create reservation
    def handleCreateReservation: Callback =
      $.state.flatMap { state =>
        (state.selectedStartDate, state.selectedEndDate) match {
          case (Some(start), Some(end)) =>
            $.modState(_.copy(isSubmitting = true, dialogError = None)) >>
            Callback.future {
              ReservationService.createReservation(start, end).map { newReservation =>
                $.modState(s => s.copy(
                  isSubmitting = false,
                  isDialogOpen = false,
                  reservations = newReservation :: s.reservations,
                  snackbarOpen = true,
                  snackbarMessage = "Reservation created successfully!",
                  selectedStartDate = None,
                  selectedEndDate = None
                ))
              }.recover {
                case e: Exception =>
                  $.modState(_.copy(
                    isSubmitting = false,
                    dialogError = Some(s"Failed to create reservation: ${e.getMessage}")
                  ))
              }
            }
          case _ =>
            Callback.empty
        }
      }
    
    // Load all reservations
    def loadReservations: Callback =
      $.modState(_.copy(isLoading = true)) >>
      Callback.future {
        ReservationService.getMyReservations.map { reservations =>
          $.modState(_.copy(
            reservations = reservations,
            isLoading = false,
            error = None
          ))
        }.recover {
          case e: Exception =>
            $.modState(_.copy(
              isLoading = false,
              error = Some(s"Failed to load reservations: ${e.getMessage}")
            ))
        }
      }
    
    // Component did mount
    val didMount: Callback = loadReservations

  val component = ScalaComponent.builder[ReservationCalendarProps]
    .initialState(ReservationCalendarState())
    .renderBackend[Backend]
    .componentDidMount(_.backend.didMount)
    .build