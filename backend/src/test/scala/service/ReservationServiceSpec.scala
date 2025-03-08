package service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.util.UUID
import java.time.{LocalDate, Instant}
import model._
import repository.ReservationRepository
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar

class ReservationServiceSpec extends AnyWordSpec with Matchers with MockitoSugar:

  "ReservationService" should {
    "create a new reservation" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      val request = ReservationRequest(
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3)
      )
      
      when(mockRepo.findOverlapping(any[LocalDate], any[LocalDate])).thenReturn(IO.pure(List.empty))
      when(mockRepo.create(any[Reservation])).thenAnswer { args =>
        val res = args.getArguments()(0).asInstanceOf[Reservation]
        IO.pure(Reservation(
          id = UUID.randomUUID(),
          userId = userId,
          startDate = res.startDate,
          endDate = res.endDate,
          createdAt = Instant.now(),
          status = ReservationStatus.Pending
        ))
      }
      
      // Act
      val result = service.createReservation(userId, request).unsafeRunSync()
      
      // Assert
      result.startDate shouldBe request.startDate
      result.endDate shouldBe request.endDate
      result.status shouldBe ReservationStatus.Pending
    }
    
    "fail creating reservation with overlapping dates" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      val existingReservation = Reservation(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(5),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      val request = ReservationRequest(
        startDate = LocalDate.now().plusDays(2),
        endDate = LocalDate.now().plusDays(4)
      )
      
      when(mockRepo.findOverlapping(any[LocalDate], any[LocalDate])).thenReturn(IO.pure(List(existingReservation)))
      
      // Act & Assert
      assertThrows[Exception] {
        service.createReservation(userId, request).unsafeRunSync()
      }
    }
    
    "fail creating reservation with invalid dates" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      
      // End date before start date
      val request1 = ReservationRequest(
        startDate = LocalDate.now().plusDays(3),
        endDate = LocalDate.now().plusDays(1)
      )
      
      // Start date in the past
      val request2 = ReservationRequest(
        startDate = LocalDate.now().minusDays(1),
        endDate = LocalDate.now().plusDays(3)
      )
      
      // Act & Assert
      assertThrows[Exception] {
        service.createReservation(userId, request1).unsafeRunSync()
      }
      
      assertThrows[Exception] {
        service.createReservation(userId, request2).unsafeRunSync()
      }
    }
    
    "get reservations for a user" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      val reservation = Reservation(
        id = UUID.randomUUID(),
        userId = userId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      when(mockRepo.findByUserId(userId)).thenReturn(IO.pure(List(reservation)))
      
      // Act
      val result = service.getUserReservations(userId).unsafeRunSync()
      
      // Assert
      result.length shouldBe 1
      result.head.id shouldBe reservation.id
    }
    
    "get a specific reservation" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val reservationId = UUID.randomUUID()
      val reservation = Reservation(
        id = reservationId,
        userId = UUID.randomUUID(),
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      when(mockRepo.findById(reservationId)).thenReturn(IO.pure(Some(reservation)))
      
      // Act
      val result = service.getReservation(reservationId).unsafeRunSync()
      
      // Assert
      result.id shouldBe reservationId
    }
    
    "fail getting non-existent reservation" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val reservationId = UUID.randomUUID()
      
      when(mockRepo.findById(reservationId)).thenReturn(IO.pure(None))
      
      // Act & Assert
      assertThrows[Exception] {
        service.getReservation(reservationId).unsafeRunSync()
      }
    }
    
    "cancel a reservation" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      val reservation = Reservation(
        id = reservationId,
        userId = userId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      val cancelledReservation = reservation.copy(status = ReservationStatus.Cancelled)
      
      when(mockRepo.findById(reservationId)).thenReturn(IO.pure(Some(reservation)))
      when(mockRepo.updateStatus(reservationId, ReservationStatus.Cancelled)).thenReturn(IO.pure(Some(cancelledReservation)))
      
      // Act
      val result = service.cancelReservation(reservationId, userId).unsafeRunSync()
      
      // Assert
      result.id shouldBe reservationId
      result.status shouldBe ReservationStatus.Cancelled
    }
    
    "fail cancelling reservation of another user" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val ownerId = UUID.randomUUID()
      val differentUserId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      val reservation = Reservation(
        id = reservationId,
        userId = ownerId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      when(mockRepo.findById(reservationId)).thenReturn(IO.pure(Some(reservation)))
      
      // Act & Assert
      assertThrows[Exception] {
        service.cancelReservation(reservationId, differentUserId).unsafeRunSync()
      }
    }
    
    "fail cancelling already cancelled reservation" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val userId = UUID.randomUUID()
      val reservationId = UUID.randomUUID()
      val reservation = Reservation(
        id = reservationId,
        userId = userId,
        startDate = LocalDate.now().plusDays(1),
        endDate = LocalDate.now().plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Cancelled
      )
      
      when(mockRepo.findById(reservationId)).thenReturn(IO.pure(Some(reservation)))
      
      // Act & Assert
      assertThrows[Exception] {
        service.cancelReservation(reservationId, userId).unsafeRunSync()
      }
    }
    
    "get reservations for a period" in {
      // Arrange
      val mockRepo = mock[ReservationRepository]
      val service = new ReservationService(mockRepo)
      
      val startDate = LocalDate.now().plusDays(1)
      val endDate = LocalDate.now().plusDays(7)
      
      val reservation = Reservation(
        id = UUID.randomUUID(),
        userId = UUID.randomUUID(),
        startDate = startDate.plusDays(1),
        endDate = startDate.plusDays(3),
        createdAt = Instant.now(),
        status = ReservationStatus.Confirmed
      )
      
      when(mockRepo.findAllBetweenDates(startDate, endDate)).thenReturn(IO.pure(List(reservation)))
      
      // Act
      val result = service.getReservationsForPeriod(startDate, endDate).unsafeRunSync()
      
      // Assert
      result.length shouldBe 1
      result.head.id shouldBe reservation.id
    }
  }