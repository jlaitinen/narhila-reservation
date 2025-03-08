# Cottage Reservation Service

A backend application built with Scala 3 for managing cottage reservations.

## Features

- User authentication (register, login, logout)
- User profile management
- Reservation management
  - Create reservations
  - Cancel reservations
  - View reservations
  - Check availability

## Technology Stack

- Scala 3.3.1
- HTTP4S for REST API
- Cats Effect for pure functional programming
- Doobie for database access
- Circe for JSON handling
- H2 in-memory database (can be replaced with PostgreSQL in production)
- Flyway for database migrations

## API Endpoints

### Authentication

- `POST /api/users/register` - Register a new user
- `POST /api/users/login` - Login and get JWT token
- `GET /api/users/profile` - Get current user profile (authenticated)

### Reservations

- `GET /api/reservations` - Get all reservations for current user (authenticated)
- `GET /api/reservations/{id}` - Get a specific reservation (authenticated)
- `POST /api/reservations` - Create a new reservation (authenticated)
- `DELETE /api/reservations/{id}` - Cancel a reservation (authenticated)
- `GET /api/reservations/period?startDate=XXX&endDate=YYY` - Get reservations for a specific period (authenticated)

## Running the Application

```bash
sbt run
```

The application will start on http://localhost:8080

## Default Admin User

The application comes with a default admin user:
- Username: admin
- Password: admin123

## Development

### Building the Project

```bash
sbt compile
```

### Running Tests

```bash
sbt test
```

### Creating a JAR

```bash
sbt assembly
```

## Database Schema

### Users Table

- id (UUID)
- username (String, unique)
- email (String, unique)
- password_hash (String)
- role (String)

### Reservations Table

- id (UUID)
- user_id (UUID, foreign key)
- start_date (Date)
- end_date (Date)
- created_at (Timestamp)
- status (String: Pending, Confirmed, Cancelled)