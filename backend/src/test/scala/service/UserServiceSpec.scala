package service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import java.util.UUID
import model._
import repository.UserRepository
import org.mockito.Mockito
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatestplus.mockito.MockitoSugar
import org.mindrot.jbcrypt.BCrypt

class UserServiceSpec extends AnyWordSpec with Matchers with MockitoSugar:

  "UserService" should {
    "register a new user" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val request = UserRegistrationRequest(
        username = "testuser",
        email = "test@example.com",
        password = "password123"
      )
      
      when(mockUserRepo.findByUsername(any[String])).thenReturn(IO.pure(None))
      when(mockUserRepo.findByEmail(any[String])).thenReturn(IO.pure(None))
      when(mockUserRepo.create(any[User])).thenAnswer { args =>
        val user = args.getArguments()(0).asInstanceOf[User]
        IO.pure(User(
          id = UUID.randomUUID(),
          username = user.username,
          email = user.email,
          passwordHash = user.passwordHash,
          role = UserRole.Regular
        ))
      }
      
      // Act
      val result = userService.register(request).unsafeRunSync()
      
      // Assert
      result.username shouldBe request.username
      result.email shouldBe request.email
      BCrypt.checkpw(request.password, result.passwordHash) shouldBe true
      result.role shouldBe UserRole.Regular
    }
    
    "fail registration if username already exists" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val existingUser = User(
        id = UUID.randomUUID(),
        username = "testuser",
        email = "existing@example.com",
        passwordHash = "hash",
        role = UserRole.Regular
      )
      
      val request = UserRegistrationRequest(
        username = "testuser",
        email = "test@example.com",
        password = "password123"
      )
      
      when(mockUserRepo.findByUsername("testuser")).thenReturn(IO.pure(Some(existingUser)))
      
      // Act & Assert
      assertThrows[Exception] {
        userService.register(request).unsafeRunSync()
      }
    }
    
    "fail registration if email already exists" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val existingUser = User(
        id = UUID.randomUUID(),
        username = "existinguser",
        email = "test@example.com",
        passwordHash = "hash",
        role = UserRole.Regular
      )
      
      val request = UserRegistrationRequest(
        username = "testuser",
        email = "test@example.com",
        password = "password123"
      )
      
      when(mockUserRepo.findByUsername("testuser")).thenReturn(IO.pure(None))
      when(mockUserRepo.findByEmail("test@example.com")).thenReturn(IO.pure(Some(existingUser)))
      
      // Act & Assert
      assertThrows[Exception] {
        userService.register(request).unsafeRunSync()
      }
    }
    
    "login with valid credentials" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val passwordHash = BCrypt.hashpw("password123", BCrypt.gensalt())
      val user = User(
        id = UUID.randomUUID(),
        username = "testuser",
        email = "test@example.com",
        passwordHash = passwordHash,
        role = UserRole.Regular
      )
      
      val request = LoginRequest(
        username = "testuser",
        password = "password123"
      )
      
      when(mockUserRepo.findByUsername("testuser")).thenReturn(IO.pure(Some(user)))
      
      // Act
      val result = userService.login(request).unsafeRunSync()
      
      // Assert
      result.user.username shouldBe user.username
      result.user.email shouldBe user.email
      result.token should not be empty
    }
    
    "fail login with invalid username" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val request = LoginRequest(
        username = "nonexistent",
        password = "password123"
      )
      
      when(mockUserRepo.findByUsername("nonexistent")).thenReturn(IO.pure(None))
      
      // Act & Assert
      assertThrows[Exception] {
        userService.login(request).unsafeRunSync()
      }
    }
    
    "fail login with invalid password" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val passwordHash = BCrypt.hashpw("correctpassword", BCrypt.gensalt())
      val user = User(
        id = UUID.randomUUID(),
        username = "testuser",
        email = "test@example.com",
        passwordHash = passwordHash,
        role = UserRole.Regular
      )
      
      val request = LoginRequest(
        username = "testuser",
        password = "wrongpassword"
      )
      
      when(mockUserRepo.findByUsername("testuser")).thenReturn(IO.pure(Some(user)))
      
      // Act & Assert
      assertThrows[Exception] {
        userService.login(request).unsafeRunSync()
      }
    }
    
    "get user profile" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val userId = UUID.randomUUID()
      val user = User(
        id = userId,
        username = "testuser",
        email = "test@example.com",
        passwordHash = "hash",
        role = UserRole.Regular
      )
      
      when(mockUserRepo.findById(userId)).thenReturn(IO.pure(Some(user)))
      
      // Act
      val result = userService.getUserProfile(userId).unsafeRunSync()
      
      // Assert
      result.id shouldBe userId
      result.username shouldBe user.username
      result.email shouldBe user.email
    }
    
    "fail getting profile of non-existent user" in {
      // Arrange
      val mockUserRepo = mock[UserRepository]
      val userService = new UserService(mockUserRepo, "test-secret")
      
      val userId = UUID.randomUUID()
      
      when(mockUserRepo.findById(userId)).thenReturn(IO.pure(None))
      
      // Act & Assert
      assertThrows[Exception] {
        userService.getUserProfile(userId).unsafeRunSync()
      }
    }
  }