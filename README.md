# Job Application Manager

A small REST API to track my own job applications (company, role, source, status, dates, notes) instead of a spreadsheet. Built as a hands-on backend project to practice Spring Boot, PostgreSQL, and database version control end to end.

## Tech stack

- Java 21
- Spring Boot (Web MVC, Data JPA)
- PostgreSQL 17 (via Docker)
- Flyway (database migrations)
- MapStruct + Lombok
- Maven

## Getting started

**Requirements:** Java 21, Docker, Maven Wrapper (included).

1. Clone the repo.
2. Copy `.env.example` to `.env` and adjust the values if needed (used by Docker Compose to configure the Postgres container).
3. Start the database:
   ```bash
   docker compose up -d
   ```
4. Export the database credentials as environment variables so Spring Boot can pick them up (it reads them from the OS environment, not from `.env` directly). They must match what's in `.env`:

   Bash:
   ```bash
   export DB_USERNAME=postgres
   export DB_PASSWORD=changeme
   ```

   PowerShell:
   ```powershell
   $env:DB_USERNAME = "postgres"
   $env:DB_PASSWORD = "changeme"
   ```
5. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   Flyway runs the migration automatically on startup and creates the `job_application` table.
6. The API is available at `http://localhost:8080/api/v1/job-applications`.

## Endpoints

| Method | Path                          | Description                  |
|--------|-------------------------------|-------------------------------|
| GET    | `/api/v1/job-applications`     | List all job applications     |
| GET    | `/api/v1/job-applications/{id}`| Get one job application       |
| POST   | `/api/v1/job-applications`     | Create a job application      |
| PUT    | `/api/v1/job-applications/{id}`| Update a job application      |
| DELETE | `/api/v1/job-applications/{id}`| Delete a job application      |

## Progress log

### Week 1 — Skeleton and persistence

- Designed and implemented the `JobApplication` entity with a full CRUD flow: controller, service, repository, request/response DTOs, and MapStruct mappers.
- Set up PostgreSQL locally with Docker Compose.
- Added the first Flyway migration to create the `job_application` table — using migrations instead of Hibernate's `ddl-auto` so the database schema is version-controlled, reviewable, and repeatable across environments, rather than auto-generated and implicit.
- Manually tested every endpoint (create, list, get by id, update, delete) with a Postman collection; all requests work as expected. The collection is kept locally for now rather than committed to the repo.

### Coming next — Week 2

- Stronger input validation on request DTOs.
- Centralized, consistent error handling (validation failures, not-found, unexpected errors) so no endpoint leaks a raw stack trace.
