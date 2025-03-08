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
  implicit val read:Read[User] = Read[User]
  implicit val get:Get[User] = Get[User]
  implicit val write:Write[User] = Write[User]

  def create(user: User): IO[User] = {
    val sql = fr"INSERT INTO users (id, username, email, password_hash, role) VALUES (${user.id}, ${user.username}, ${user.email}, ${user.passwordHash}, ${user.role.toString})"
    sql.update.run.transact(xa).as(user)
  }

  def findById(id: UUID): IO[Option[User]] = {
    val sql = fr"SELECT id, username, email, password_hash, role FROM users WHERE id = $id"
    sql.query[User].option.transact(xa)
  }

  def findByUsername(username: String): IO[Option[User]] = {
    val sql = fr"SELECT id, username, email, password_hash, role FROM users WHERE username = $username"
    sql.query[User].option.transact(xa)
  }

  def findByEmail(email: String): IO[Option[User]] = {
    val sql = fr"SELECT id, username, email, password_hash, role FROM users WHERE email = $email"
    sql.query[User].option.transact(xa)
  }

  def listAll(): IO[List[User]] = {
    val sql = fr"SELECT id, username, email, password_hash, role FROM users"
    sql.query[User].to[List].transact(xa)
  }

  def update(user: User): IO[Option[User]] = {
    val sql = fr"""
      UPDATE users
      SET username = ${user.username},
          email = ${user.email},
          password_hash = ${user.passwordHash},
          role = ${user.role.toString}
      WHERE id = ${user.id}
    """
    sql.update.run.transact(xa).flatMap { rows =>
      if (rows > 0) IO.pure(Some(user)) else IO.pure(None)
    }
  }

  def delete(id: UUID): IO[Boolean] = {
    val sql = fr"DELETE FROM users WHERE id = $id"
    sql.update.run.transact(xa).map(_ > 0)
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