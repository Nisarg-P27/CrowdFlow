# CrowdFlow

> A production-style event booking backend built with Spring Boot — featuring secure auth, ticket management, payment processing, Redis caching, and organizer analytics.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Core Domain Model](#core-domain-model)
- [Security](#security)
- [Caching](#caching)
- [Rate Limiting](#rate-limiting)
- [Database Migrations](#database-migrations)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Key Engineering Decisions](#key-engineering-decisions)
- [Future Improvements](#future-improvements)
- [License](#license)

---

## Features

### Authentication & Authorization
- User registration and login with JWT-based authentication
- Refresh token rotation for session security
- Role-based access control (RBAC)
- Secure logout support

### Event Management
- Create, update, and delete events
- Browse events with pagination
- Organizer ownership validation

### Ticket Booking
- Book tickets with automatic seat availability management
- Booking cancellation and history retrieval
- Transactional booking workflow

### Payments
- Razorpay order creation and payment verification
- Webhook-based payment status updates
- Booking confirmation after successful payment

### Organizer Dashboard
- Event performance statistics
- Revenue metrics and booking analytics

### Performance & Reliability
- Redis caching for frequently accessed data
- Distributed rate limiting with Bucket4j
- Optimistic locking for concurrent seat updates
- Flyway database migrations
- Global exception handling

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3, Spring Security, Spring Data JPA |
| ORM | Hibernate |
| Database | PostgreSQL |
| Migrations | Flyway |
| Caching | Redis |
| Auth | JWT + Refresh Tokens |
| Payments | Razorpay |
| Testing | JUnit 5, Mockito, Testcontainers |
| Docs | OpenAPI / Swagger |

---

## Architecture

CrowdFlow follows a clean layered architecture:

```
controllers  →  services  →  repositories  →  database
```

Supporting layers:

```
configs / security / exceptions / dtos / contracts / infrastructure
```

---

## Core Domain Model

| Entity | Description |
|---|---|
| `User` | Represents platform users and organizers |
| `Event` | Stores event information and seat availability |
| `Booking` | Represents a ticket reservation made by a user |
| `Payment` | Tracks payment lifecycle and provider details |
| `RefreshToken` | Manages authenticated user sessions |

---

## Security

- JWT access tokens for stateless authentication
- Refresh token rotation to reduce replay risk
- Role-based endpoint protection
- Redis-backed session storage

---

## Caching

Frequently accessed event data is cached using Redis:

- Event details
- Event listings

Cache entries are automatically invalidated when event data changes.

---

## Rate Limiting

Bucket4j combined with Redis protects against abuse:

- Authentication endpoint throttling
- Configurable per-endpoint request limits

---

## Database Migrations

Schema changes are version-controlled with Flyway. Migration scripts live at:

```
src/main/resources/db/migration
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven
- PostgreSQL
- Redis

### Clone the Repository

```bash
git clone https://github.com/your-username/crowdflow.git
cd crowdflow
```

### Configure Environment

Update `application.properties` with your credentials:

```properties
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=

jwt.secret=

redis.host=
redis.port=

razorpay.key-id=
razorpay.key-secret=
```

### Run the Application

```bash
mvn spring-boot:run
```

The application starts at `http://localhost:8080`.

---

## API Documentation

| Resource | URL |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI Spec | `http://localhost:8080/v3/api-docs` |

---

## Testing

```bash
mvn test
```

The test suite covers:

- Unit tests
- Integration tests
- Authentication flows
- Booking workflows
- Rate limiting behavior

---

## Key Engineering Decisions

### Refresh Token Rotation
Every refresh invalidates the previous token and issues a new one, significantly reducing replay attack risk.

### Optimistic Locking
Seat updates use optimistic locking to safely handle concurrent booking requests without pessimistic locks slowing throughput.

### Redis Abstractions
Redis access is hidden behind contracts and infrastructure implementations, keeping business logic storage-agnostic and easier to test.

### Provider-Based Payment Integration
Payment processing sits behind a provider interface, making it straightforward to add new payment gateways without touching core booking logic.

---

## Future Improvements

- [ ] Email notifications for bookings and cancellations
- [ ] PDF ticket generation
- [ ] Multi-provider payment support
- [ ] Advanced event search and filtering
- [ ] Admin dashboard
- [ ] Docker / Docker Compose support

---

## License

This project is intended for educational and portfolio purposes.
