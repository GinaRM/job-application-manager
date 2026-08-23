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
- Manually tested every endpoint (create, list, get by id, update, delete) with a Postman collection covering the happy path plus edge cases: validation failure, an attempt to set `status` on creation, not-found, an invalid enum value, and deleting the same resource twice. Collection lives in [`/postman`](./postman).

### Coming next — Week 2

- Stronger input validation on request DTOs.
- Centralized, consistent error handling (validation failures, not-found, unexpected errors) so no endpoint leaks a raw stack trace.

## API testing

The Postman collection in [`/postman`](./postman) imports directly (variables `baseUrl` and `applicationId` are set up already — `applicationId` fills itself in after running the "create" request). Each request has test assertions attached, so **Run Collection** doubles as a quick regression check, not just a way to fire requests manually.

## Design decisions

This section is deliberately not a description of what the code does — it's *why it's built this way*, including the alternatives that were considered and turned down. That's what actually gets asked about in an interview.

### Layered architecture instead of Clean/Hexagonal

**Decision:** a straightforward controller → service → repository flow, with DTOs and a mapper at the boundary.

**Alternative considered:** Clean/Hexagonal Architecture — ports and adapters, use-case interactor classes, a domain model fully isolated behind interfaces from persistence and delivery.

**Why:** Hexagonal architecture earns its cost when a domain has real business complexity, or when infrastructure genuinely needs to be swappable (multiple persistence backends, multiple delivery mechanisms, adapters that get replaced independently). This domain is one entity with seven fields and a single business rule (the initial status). Adding ports, interactors, and a separate domain model on top of that would mean more files and more indirection for every future change, without solving a problem this project actually has — there's no complexity to isolate and nothing planned that needs to be swapped out. The extra layer is a cost paid in reading, reviewing, and onboarding time, and right now nothing buys it back. If the domain grows real complexity later (interview scheduling logic, multiple external integrations), this is worth revisiting — it's a decision sized to the current scope, not a rejection of the pattern on principle.

### The initial status is not accepted from the client

**Decision:** `JobApplicationRequest` (create) has no `status` field. A new `JobApplication` always starts as `APPLIED`, regardless of what's in the request body.

**Why:** "a new application starts as `APPLIED`" is a business rule, not a client preference — it has to hold no matter which caller hits the endpoint (this controller today, a future frontend, a script, anyone). If `status` were a settable field on the request, that rule would only exist because callers happen to behave, which isn't enforcement. Leaving the field out of the DTO enforces it at the API boundary itself — there's no field to send, so there's nothing to bypass with. Postman test 03 checks exactly this: even a request body with `"status": "OFFERED"` still comes back as `APPLIED`.

### A static factory method instead of a public builder, for the entity

**Decision:** new `JobApplication` instances are created through `JobApplication.create(companyName, roleTitle, source, appliedOn, notes)`.

**Alternative considered:** exposing the Lombok-generated `@Builder` as the way to construct new entities.

**Why:** a builder lets a caller set any field in any combination — including fields that shouldn't be caller-controlled at creation time (`status`, see above), or skip a field entirely by accident. A factory method with a fixed parameter list is the one guarded entry point that decides what "a new, valid `JobApplication`" looks like; there's no path that skips it. In other words: a builder makes invalid states representable, and this removes that.

**Open item:** `@Builder` is currently still public on the entity, so `JobApplication.builder()...build()` still works as an unguarded side door — it wasn't restricted when the builder was added for `toBuilder()`/mapping use. To make this decision hold in practice, not just in intent, the builder's visibility should be narrowed (e.g. `@Builder(access = AccessLevel.PACKAGE)`).

### `JobApplicationService` has no interface

**Decision:** `JobApplicationService` is a concrete class; there's no `JobApplicationService` interface with an `Impl` behind it.

**Alternative considered:** the classic Spring `Service` / `ServiceImpl` interface-plus-implementation pair.

**Why:** that pattern exists for two historical reasons, and neither applies to this stack anymore. First, Spring used to require an interface so `@Transactional` and friends could be applied via JDK dynamic proxies — Spring has proxied concrete classes with CGLIB by default for years, so a class works fine. Second, older mocking frameworks needed an interface to generate a test double — Mockito has mocked concrete classes directly (via bytecode generation) since version 2. With exactly one implementation and nothing being swapped, an interface here would just be a second file to keep in sync with every method signature change, buying no actual polymorphism. If a second implementation is ever genuinely needed, extracting an interface at that point is a trivial, safe refactor.

### DTOs instead of exposing the JPA entity

**Decision:** the API never accepts or returns `JobApplication` directly — always `JobApplicationRequest` / `JobApplicationUpdateRequest` / `JobApplicationResponse`.

**Alternative considered:** bind the controller directly to the entity and skip the mapping layer.

**Why:** the entity and the public API contract are two different things that currently happen to look similar, and binding them together directly causes two separate problems. Going in: if the controller deserialized request bodies straight into `JobApplication`, every entity field becomes settable by the client by default (mass assignment) — a request could set `id` or `status` directly, silently bypassing the invariant above, because nothing stops JSON keys from binding to matching entity fields. Going out: returning the entity ties the public JSON shape to the persistence model, so an internal change — a column rename, a new relationship, a lazily-loaded field — becomes a breaking API change (or a serialization bug) for every consumer, instead of an internal detail. DTOs make the input/output shape an explicit contract instead of an accidental side effect of the schema.

### `PUT` replaces the whole resource, and that's why the update DTO differs from the create DTO

**Decision:** `PUT /api/v1/job-applications/{id}` requires the full representation — `companyName`, `roleTitle`, `appliedOn`, and `status` again, not just the field being changed. A field left out of the body (e.g. `notes`) gets cleared, not left untouched (Postman test 08 checks this directly).

**Alternative considered:** treat omitted fields as "leave unchanged" (PATCH-like behavior on the `PUT` endpoint).

**Why full replacement:** `PUT` has a specific meaning in HTTP — "here is the complete representation of this resource, make the server match it" — and that's exactly what makes it idempotent: sending the same `PUT` twice produces the same result. A `PUT` that partially applies fields isn't standard `PUT` or `PATCH`; it creates an ambiguity on every omitted field ("unchanged" or "clear it"?) that the caller can't resolve without reading the server's source. Requiring the full body removes that ambiguity, at the cost of the caller resending fields it isn't changing — which is the trade `PUT` is supposed to make. `PATCH` is the verb for true partial updates, and is a reasonable future addition if that need shows up.

**Why the update DTO has `status` and the create DTO doesn't:** this follows directly from the earlier decision. On creation there's no status decision to make — the domain rule fixes it to `APPLIED`. On update, the application already exists, and moving it through its lifecycle (`APPLIED` → `INTERVIEWING` → `OFFERED`/`REJECTED`) is the whole point of updating it — so `status` has to be an input there. The two DTOs differ because they represent different moments in the entity's lifecycle, not because one was written carelessly.

## Scalability considerations

None of this is wired up yet, on purpose — it's sized for one user (me) hitting it from Postman, not production traffic. Here's specifically what's missing and why it matters:

- **No pagination.** `GET /api/v1/job-applications` loads and returns every row on every call. Fine at a handful of records; both response size and query cost grow linearly with the table, with no way for a caller to ask for "just page 3." Needs to become a `Pageable`-based query (page/size/sort as query params) before it could handle a real dataset.
- **No caching.** Every `GET` hits PostgreSQL directly, every time, even for data that hasn't changed between requests. For one user that's irrelevant; under real traffic the database becomes the bottleneck first, doing repeat work for identical reads that a read-through cache (in-process, or Redis if this ever runs as more than one instance) would absorb.
- **Only one index exists** (`idx_job_application_status`), and it's worth being precise about what that buys and what it doesn't. An index lets Postgres jump to matching rows instead of scanning the whole table. `idx_job_application_status` speeds up any query filtered by status (`WHERE status = 'INTERVIEWING'`), which is a reasonable bet since "show me my active applications" is a near-certain future query. It does nothing for other access patterns — sorting by `appliedOn`, or searching by `companyName`, still forces a full scan today, because no index covers them. That's not an oversight so much as a sequencing choice: every index speeds up reads on its column but slows down every insert/update (the index has to be maintained) and costs disk space, so adding indexes ahead of an actual query that needs them is guessing. Further indexes belong in the migration that ships the endpoint that actually queries by them.
- **No rate limiting.** Nothing stops a single caller — a buggy script, a retry loop, a scraper — from sending requests as fast as the network allows. Irrelevant while this only runs locally; the moment it's reachable from anywhere, one client can degrade or exhaust the service for everyone (or run up database load). This is a service-availability/fairness concern, not an auth concern, and is normally handled at the edge (gateway/reverse proxy) or in-app (e.g. Bucket4j, keyed by API key or IP).
- **No observability.** No structured logging, no metrics, no request tracing, no correlation ID tying a request to what happened downstream. Right now, if something breaks in production, the only signal is an external symptom (a failed request) with no way to find out why from the server side — no log line to grep, no metric to alert on, no trace to follow. Closing this gap — structured JSON logs, Spring Boot Actuator (health/metrics), and a per-request correlation ID — is planned for a later stage of the project, once the core API and its tests are solid.
