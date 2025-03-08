package config

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import doobie._
import doobie.implicits._
import cats.effect.unsafe.implicits.global

class DatabaseSpec extends AsyncWordSpec with AsyncIOSpec with Matchers:

  "Database" should {
    "initialize with Flyway migrations" in {
      // Create test database configuration
      val testConfig = DbConfig(
        driver = "org.h2.Driver",
        url = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        user = "sa",
        password = "",
        maxConnections = 4
      )
      
      // Test program to initialize database and check tables
      val testProgram = Database.transactor(testConfig).use { xa =>
        for {
          // Initialize database with Flyway
          _ <- Database.initialize(xa)
          
          // Check if tables were created correctly
          userTableExists <- sql"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'USERS'"
            .query[Int].unique.transact(xa)
          
          reservationTableExists <- sql"SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'RESERVATIONS'"
            .query[Int].unique.transact(xa)
            
          // Check if admin user was created
          adminExists <- sql"SELECT COUNT(*) FROM USERS WHERE USERNAME = 'admin'"
            .query[Int].unique.transact(xa)
        } yield (userTableExists, reservationTableExists, adminExists)
      }
      
      testProgram.asserting { case (userTableExists, reservationTableExists, adminExists) =>
        userTableExists shouldBe 1
        reservationTableExists shouldBe 1
        adminExists shouldBe 1
      }
    }
  }