package repository

import cats.effect.{IO, Resource}
import doobie._
import doobie.implicits._
import doobie.util.meta.Meta
import doobie.implicits.javasql._
import doobie.postgres.implicits._
import doobie.h2._
import doobie.free.connection.ConnectionIO
import doobie.hikari.HikariTransactor
import model.{User, UserRole}
import java.util.UUID

trait UserRepository:
  def create(user: User): IO[User]
  def findById(id: UUID): IO[Option[User]]
  def findByUsername(username: String): IO[Option[User]]
  def findByEmail(email: String): IO[Option[User]]
  def listAll(): IO[List[User]]
  def update(user: User): IO[Option[User]]
  def delete(id: UUID): IO[Boolean]

class DoobieUserRepository(xa: Transactor[IO]) extends UserRepository:
  import UserRepository._
  // Simple SQL queries with explicit column mapping
  private def userFromRow(id: UUID, username: String, email: String, passwordHash: String, roleStr: String): User = {
    val role = if (roleStr == "Admin") UserRole.Admin else UserRole.Regular
    User(id, username, email, passwordHash, role)
  }

  def create(user: User): IO[User] = {
    val sql = fr"INSERT INTO users (id, username, email, password_hash, role) VALUES (${user.id}, ${user.username}, ${user.email}, ${user.passwordHash}, ${user.role.toString})"
    sql.update.run.transact(xa).as(user)
  }

  def findById(id: UUID): IO[Option[User]] = {
    val idStr = id.toString
    val sql = fr"""
      SELECT id, username, email, password_hash, role 
      FROM users 
      WHERE id = $idStr
    """
    sql.query[(String, String, String, String, String)]
      .map { case (idStr, username, email, passwordHash, roleStr) =>
        try {
          val uuid = UUID.fromString(idStr)
          userFromRow(uuid, username, email, passwordHash, roleStr)
        } catch {
          case e: Exception =>
            IO(System.err.println(s"Invalid UUID format: $idStr, error: ${e.getMessage}"))
            User(id, username, email, passwordHash, if (roleStr == "Admin") UserRole.Admin else UserRole.Regular)
        }
      }
      .option
      .transact(xa)
      .handleErrorWith(e => {
        IO(System.err.println(s"Database error in findById: ${e.getMessage}")) *> 
        IO.pure(None)
      })
  }

  def findByUsername(username: String): IO[Option[User]] = {
    if (username == null || username.trim.isEmpty) {
      IO.pure(None)
    } else {
      val sql = fr"""
        SELECT id, username, email, password_hash, role 
        FROM users 
        WHERE username = $username
      """
      sql.query[(String, String, String, String, String)]
        .map { case (idStr, username, email, passwordHash, roleStr) =>
          try {
            val id = UUID.fromString(idStr)
            userFromRow(id, username, email, passwordHash, roleStr)
          } catch {
            case e: Exception =>
              IO(System.err.println(s"Invalid UUID: $idStr, error: ${e.getMessage}"))
              User(
                UUID.randomUUID(),  // Fallback UUID
                username,
                email,
                passwordHash,
                if (roleStr == "Admin") UserRole.Admin else UserRole.Regular
              )
          }
        }
        .option
        .transact(xa)
        .handleErrorWith(e => {
          IO(System.err.println(s"Database error in findByUsername: ${e.getMessage}")) *> 
          IO.pure(None)
        })
    }
  }

  def findByEmail(email: String): IO[Option[User]] = {
    if (email == null || email.trim.isEmpty) {
      IO.pure(None)
    } else {
      val sql = fr"""
        SELECT id, username, email, password_hash, role 
        FROM users 
        WHERE email = $email
      """
      sql.query[(String, String, String, String, String)]
        .map { case (idStr, username, email, passwordHash, roleStr) =>
          try {
            val id = UUID.fromString(idStr)
            userFromRow(id, username, email, passwordHash, roleStr)
          } catch {
            case e: Exception =>
              IO(System.err.println(s"Invalid UUID: $idStr, error: ${e.getMessage}"))
              User(
                UUID.randomUUID(),  // Fallback UUID
                username,
                email,
                passwordHash,
                if (roleStr == "Admin") UserRole.Admin else UserRole.Regular
              )
          }
        }
        .option
        .transact(xa)
        .handleErrorWith(e => {
          IO(System.err.println(s"Database error in findByEmail: ${e.getMessage}")) *> 
          IO.pure(None)
        })
    }
  }

  def listAll(): IO[List[User]] = {
    val sql = fr"SELECT id, username, email, password_hash, role FROM users"
    sql.query[(String, String, String, String, String)]
      .map { case (idStr, username, email, passwordHash, roleStr) =>
        try {
          val id = UUID.fromString(idStr)
          userFromRow(id, username, email, passwordHash, roleStr)
        } catch {
          case e: Exception =>
            IO(System.err.println(s"Invalid UUID: $idStr, error: ${e.getMessage}"))
            User(
              UUID.randomUUID(),  // Fallback UUID
              username,
              email,
              passwordHash,
              if (roleStr == "Admin") UserRole.Admin else UserRole.Regular
            )
        }
      }
      .to[List]
      .transact(xa)
      .handleErrorWith(e => {
        IO(System.err.println(s"Database error in listAll: ${e.getMessage}")) *> 
        IO.pure(List.empty)
      })
  }

  def update(user: User): IO[Option[User]] = {
    if (user == null || user.id == null) {
      IO.pure(None)
    } else {
      val idStr = user.id.toString
      val sql = fr"""
        UPDATE users
        SET username = ${user.username},
            email = ${user.email},
            password_hash = ${user.passwordHash},
            role = ${user.role.toString}
        WHERE id = $idStr
      """
      sql.update.run.transact(xa).flatMap { rows =>
        if (rows > 0) IO.pure(Some(user)) else IO.pure(None)
      }.handleErrorWith(e => {
        IO(System.err.println(s"Database error in update: ${e.getMessage}")) *> 
        IO.pure(None)
      })
    }
  }

  def delete(id: UUID): IO[Boolean] = {
    val idStr = id.toString
    val sql = fr"DELETE FROM users WHERE id = $idStr"
    sql.update.run.transact(xa).map(_ > 0)
      .handleErrorWith(e => {
        IO(System.err.println(s"Database error in delete: ${e.getMessage}")) *> 
        IO.pure(false)
      })
  }

object UserRepository:
  // Meta instance for UUID
  given Meta[UUID] = Meta[String].imap(UUID.fromString)(_.toString)
  
  // Meta instance for UserRole
  given Meta[UserRole] = Meta[String].imap {
    case "Regular" => UserRole.Regular
    case "Admin" => UserRole.Admin
    case role => throw new IllegalArgumentException(s"Unknown role: $role")
  }(_.toString)