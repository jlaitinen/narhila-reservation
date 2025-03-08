import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSImport, JSGlobal}
import pages.App
import japgolly.scalajs.react.ReactDOM

object Main:
  def main(args: Array[String]): Unit =
    println("Starting Cottage Reservation Frontend")
    
    // Wait for DOM to be loaded
    dom.document.addEventListener("DOMContentLoaded", (_: dom.Event) => {
      // Get the root element
      val rootElement = dom.document.getElementById("root")
      
      // Render the app using scalajs-react
      App.component().renderIntoDOM(rootElement)
    })