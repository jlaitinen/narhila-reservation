package services

import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Encoder, Decoder}
import org.scalajs.dom
import shared.api.ApiRoutes

import scala.concurrent.{Future, Promise}
import concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import scala.scalajs.js
import scala.scalajs.js.Thenable.Implicits._

object ApiService:
  private def headers(token: Option[String] = None): dom.Headers =
    val h = new dom.Headers()
    h.append("Content-Type", "application/json")
    token.foreach(t => h.append("Authorization", s"Bearer $t"))
    h

  def get[T](url: String, token: Option[String] = None)(using d: Decoder[T]): Future[T] =
    val promise = Promise[T]()
    val requestInit = js.Dynamic.literal(
      method = "GET",
      headers = headers(token),
      mode = "cors"
    ).asInstanceOf[dom.RequestInit]

    dom.fetch(url, requestInit)
      .flatMap(_.text())
      .map { text =>
        decode[T](text) match
          case Right(value) => promise.success(value)
          case Left(error) => promise.failure(new Exception(s"Failed to decode response: $error"))
      }
      .recover { case e =>
        promise.failure(new Exception(s"Failed to fetch data: ${e.getMessage}"))
      }

    promise.future

  def post[T, R](url: String, body: T, token: Option[String] = None)(using d: Decoder[R], e: Encoder[T]): Future[R] =
    val promise = Promise[R]()
    val requestInit = js.Dynamic.literal(
      method = "POST",
      headers = headers(token),
      body = body.asJson.noSpaces,
      mode = "cors"
    ).asInstanceOf[dom.RequestInit]

    dom.fetch(url, requestInit)
      .flatMap(response => 
        if (response.ok) {
          response.text().map(Right(_))
        } else {
          // Handle error responses
          response.text().map(errorText => {
            try {
              // Try to parse error as JSON
              val errorJson = js.JSON.parse(errorText).asInstanceOf[js.Dynamic]
              val errorMsg = Option(errorJson.error.asInstanceOf[js.UndefOr[String]])
                .flatMap(_.toOption)
                .getOrElse("Unknown error")
              Left(s"API Error: $errorMsg")
            } catch {
              case _: Throwable => Left(s"API Error: ${response.statusText}")
            }
          })
        }
      )
      .map {
        case Right(text) =>
          decode[R](text) match
            case Right(value) => promise.success(value)
            case Left(error) => promise.failure(new Exception(s"Failed to decode response: $error"))
        case Left(errorMsg) =>
          promise.failure(new Exception(errorMsg))
      }
      .recover { case e =>
        promise.failure(new Exception(s"Failed to post data: ${e.getMessage}"))
      }

    promise.future

  def delete[T](url: String, token: Option[String] = None)(using d: Decoder[T]): Future[T] =
    val promise = Promise[T]()
    val requestInit = js.Dynamic.literal(
      method = "DELETE",
      headers = headers(token),
      mode = "cors"
    ).asInstanceOf[dom.RequestInit]

    dom.fetch(url, requestInit)
      .flatMap(_.text())
      .map { text =>
        decode[T](text) match
          case Right(value) => promise.success(value)
          case Left(error) => promise.failure(new Exception(s"Failed to decode response: $error"))
      }
      .recover { case e =>
        promise.failure(new Exception(s"Failed to delete data: ${e.getMessage}"))
      }

    promise.future