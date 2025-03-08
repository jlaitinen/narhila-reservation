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
import java.time.{LocalDate, Instant}
import model._
import service.{ReservationService, UserService}
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar

class ReservationRoutesSpec extends AnyWordSpec with Matchers with MockitoSugar:

  // Json codecs for HTTP
  implicit val reservationViewDecoder: EntityDecoder[IO, ReservationView] = jsonOf[IO, ReservationView]
  implicit val reservationViewListDecoder: EntityDecoder[IO, List[ReservationView]] = jsonOf[IO, List[ReservationView]]
  implicit val reservationRequestEncoder: EntityEncoder[IO, ReservationRequest] = jsonEncoderOf[IO, ReservationRequest]

  "ReservationRoutes" should {
    "list user reservations" in {
      // Arrange
      val mockUserService = mock[UserService]
      val mockReservationService = mock[ReservationService]
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      
      val reservationView = ReservationView(
        id = reservationId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        status = ReservationStatus.Confirmed,
        createdAt = Instant.now()
      )
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockReservationService.getUserReservations(any[UUID])).thenReturn(IO.pure(List(reservationView)))
      
      val authMiddleware = AuthMiddleware.authUser(mockUserService)
      val routes = ReservationRoutes.routes(mockReservationService, authMiddleware)
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.GET, uri = uri"/api/reservations")
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val reservations = response.as[List[ReservationView]].unsafeRunSync()
      reservations.length shouldBe 1
      reservations.head.id shouldBe reservationId
    }
    
    "create a new reservation" in {
      // Arrange
      val mockUserService = mock[UserService]
      val mockReservationService = mock[ReservationService]
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      
      val reservationRequest = ReservationRequest(
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3)
      )
      
      val reservationView = ReservationView(
        id = reservationId,
        startDate = reservationRequest.startDate,
        endDate = reservationRequest.endDate,
        status = ReservationStatus.Pending,
        createdAt = Instant.now()
      )
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockReservationService.createReservation(any[UUID], any[ReservationRequest])).thenReturn(IO.pure(reservationView))
      
      val authMiddleware = AuthMiddleware.authUser(mockUserService)
      val routes = ReservationRoutes.routes(mockReservationService, authMiddleware)
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.POST, uri = uri"/api/reservations")
          .withEntity(reservationRequest)
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Created
      val reservation = response.as[ReservationView].unsafeRunSync()
      reservation.id shouldBe reservationId
      reservation.startDate shouldBe reservationRequest.startDate
      reservation.endDate shouldBe reservationRequest.endDate
    }
    
    "get a specific reservation" in {
      // Arrange
      val mockUserService = mock[UserService]
      val mockReservationService = mock[ReservationService]
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      
      val reservationView = ReservationView(
        id = reservationId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        status = ReservationStatus.Confirmed,
        createdAt = Instant.now()
      )
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockReservationService.getReservation(any[UUID])).thenReturn(IO.pure(reservationView))
      
      val authMiddleware = AuthMiddleware.authUser(mockUserService)
      val routes = ReservationRoutes.routes(mockReservationService, authMiddleware)
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.GET, Uri.unsafeFromString(s"/api/reservations/${reservationId.toString}"))
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val reservation = response.as[ReservationView].unsafeRunSync()
      reservation.id shouldBe reservationId
    }
    
    "cancel a reservation" in {
      // Arrange
      val mockUserService = mock[UserService]
      val mockReservationService = mock[ReservationService]
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      
      val reservationView = ReservationView(
        id = reservationId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        status = ReservationStatus.Cancelled,
        createdAt = Instant.now()
      )
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockReservationService.cancelReservation(any[UUID], any[UUID])).thenReturn(IO.pure(reservationView))
      
      val authMiddleware = AuthMiddleware.authUser(mockUserService)
      val routes = ReservationRoutes.routes(mockReservationService, authMiddleware)
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.DELETE, Uri.unsafeFromString(s"/api/reservations/${reservationId.toString}"))
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val reservation = response.as[ReservationView].unsafeRunSync()
      reservation.status shouldBe ReservationStatus.Cancelled
    }
    
    "search reservations by period" in {
      // Arrange
      val mockUserService = mock[UserService]
      val mockReservationService = mock[ReservationService]
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      
      val startDate = LocalDate.now().plusDays(1)
      val endDate = LocalDate.now().plusDays(10)
      
      val reservationView = ReservationView(
        id = reservationId,
        startDate = startDate.plusDays(2),
        endDate = startDate.plusDays(4),
        status = ReservationStatus.Confirmed,
        createdAt = Instant.now()
      )
      
      when(mockUserService.validateToken(any[String])).thenReturn(IO.pure(userId))
      when(mockReservationService.getReservationsForPeriod(any[LocalDate], any[LocalDate])).thenReturn(IO.pure(List(reservationView)))
      
      val authMiddleware = AuthMiddleware.authUser(mockUserService)
      val routes = ReservationRoutes.routes(mockReservationService, authMiddleware)
      
      // Act
      val response = routes.orNotFound.run(
        Request(method = Method.GET, 
                Uri.unsafeFromString(s"/api/reservations/period?startDate=${startDate.toString}&endDate=${endDate.toString}"))
          .withHeaders(Headers(Header("Authorization", "Bearer fake-token")))
      ).unsafeRunSync()
      
      // Assert
      response.status shouldBe Status.Ok
      val reservations = response.as[List[ReservationView]].unsafeRunSync()
      reservations.length shouldBe 1
      reservations.head.id shouldBe reservationId
    }
  }