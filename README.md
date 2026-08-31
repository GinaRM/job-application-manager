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
2. Copy `.env.example` to `.env` and adjust the values if needed. Docker Compose reads this file directly to configure the Postgres container — that's also where `DB_PORT=5433` comes from instead of Postgres's default `5432`. That's intentional, not a typo: publishing on `5433` avoids clashing with a Postgres instance already installed natively on your machine, which is worth knowing before you lose time chasing a "port already in use" error.
3. Start the database:
   ```bash
   docker compose up -d
   ```
4. Export the same database credentials as environment variables so Spring Boot can pick them up. This is a separate step from step 2, not a duplicate of it: Docker Compose reads `.env` itself, but Spring Boot never opens that file — it only reads the OS environment. Two different mechanisms read config from two different places, so the same values have to be provided to both. They must match what's in `.env`:

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

| Method | Path                             | Description                | Success | Errors    |
|--------|----------------------------------|-----------------------------|---------|-----------|
| GET    | `/api/v1/job-applications`       | List all job applications   | 200     | —         |
| GET    | `/api/v1/job-applications/{id}`  | Get one job application     | 200     | 404       |
| POST   | `/api/v1/job-applications`       | Create a job application    | 201     | 400       |
| PUT    | `/api/v1/job-applications/{id}`  | Update a job application    | 200     | 400, 404  |
| DELETE | `/api/v1/job-applications/{id}`  | Delete a job application    | 204     | 404       |

### Interviews (nested under a job application)

| Method | Path                                                          | Description                        | Success | Errors    |
|--------|----------------------------------------------------------------|-------------------------------------|---------|-----------|
| GET    | `/api/v1/job-applications/{applicationId}/interviews`          | List interviews for an application  | 200     | —         |
| GET    | `/api/v1/job-applications/{applicationId}/interviews/{id}`     | Get one interview                   | 200     | 404       |
| POST   | `/api/v1/job-applications/{applicationId}/interviews`          | Schedule an interview               | 201     | 400, 404  |
| PUT    | `/api/v1/job-applications/{applicationId}/interviews/{id}`     | Update an interview                 | 200     | 400, 404  |
| DELETE | `/api/v1/job-applications/{applicationId}/interviews/{id}`     | Delete an interview                 | 204     | 404       |

`POST` returns 400 on validation failure like every other write endpoint, plus its own 404 if the parent `{applicationId}` doesn't exist. On every other route, `{id}` is resolved together with `{applicationId}` (`findByIdAndApplicationId` / `deleteByIdAndApplicationId`), so an interview that exists but belongs to a different application also 404s — see [Design decisions](#design-decisions).

### Example: create a job application

Request — `POST /api/v1/job-applications`

```json
{
  "companyName": "Globant",
  "roleTitle": "Java Backend Developer",
  "source": "LinkedIn",
  "appliedOn": "2026-08-20",
  "notes": "Remote role, Spring Boot stack"
}
```

Response — `201 Created` (with a `Location: /api/v1/job-applications/1` header)

```json
{
  "id": 1,
  "companyName": "Globant",
  "roleTitle": "Java Backend Developer",
  "source": "LinkedIn",
  "status": "APPLIED",
  "appliedOn": "2026-08-20",
  "notes": "Remote role, Spring Boot stack"
}
```

Note there's no `status` in the request — see [Design decisions](#design-decisions) for why.

## Progress log

### Week 1 — Skeleton and persistence

- Designed and implemented the `JobApplication` entity with a full CRUD flow: controller, service, repository, request/response DTOs, and a MapStruct mapper.
- Set up PostgreSQL locally with Docker Compose.
- Added the first Flyway migration to create the `job_application` table — using migrations instead of Hibernate's `ddl-auto` so the database schema is version-controlled, reviewable, and repeatable across environments, rather than auto-generated and implicit.
- Manually tested every endpoint (create, list, get by id, update, delete) with a Postman collection covering the happy path plus edge cases: validation failure, an attempt to set `status` on creation, not-found, an invalid enum value, and deleting the same resource twice. Collection lives in [`/postman`](./postman).

### Week 2 — Layers, errors, and validation

- Added the `Interview` entity with a nested CRUD (`/api/v1/job-applications/{applicationId}/interviews`), including its own request/response DTOs, MapStruct mapper, service, repository, and controller — same layering discipline as `JobApplication`.
- Tightened Bean Validation on every request DTO, both existing and new.
- Replaced the ad-hoc `ErrorResponse` with `ProblemDetail` (RFC 9457, the 2023 revision of the original RFC 7807) inside `GlobalExceptionHandler`, so every error path — validation, not-found, unreadable body, unexpected exception — returns the same consistent shape instead of a per-exception one-off.
- Renamed `JobApplicationNotFoundException` to `ResourceNotFoundException` and made it take a resource name, so `Interview` reuses the same 404 path instead of a second entity-specific exception class.
- Several design decisions this week — nested routes, `@ManyToOne`-only relation, ownership-checked lookups, cascading delete, and the `PENDING` factory default — see [Design decisions](#design-decisions).
- Added a second Postman collection dedicated to interviews, covering the nested CRUD plus a deliberate cross-application isolation test.

## API testing

Two Postman collections live in [`/postman`](./postman), both import directly with variables pre-wired and test assertions on every request, so **Run Collection** doubles as a quick regression check:

- **`job-application-manager.postman_collection.json`** — the job application CRUD (`baseUrl` and `applicationId`, the latter fills itself in after the "create" request).
- **`interviews.postman_collection.json`** — the nested interview CRUD. Request `00` creates a job application and request `01` creates an interview under it, filling in `applicationId` and `interviewId` for the rest of the collection; a second application (`otherApplicationId`) is created specifically to test that an interview looked up through the wrong `applicationId` 404s instead of leaking across applications.

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

### Nested routes for interviews, because an interview doesn't exist without an application

**Decision:** every interview endpoint hangs off `/api/v1/job-applications/{applicationId}/interviews`, never a flat `/api/v1/interviews`.

**Why:** an interview is not an independent resource — it's always a step inside one specific application's process, and it has no meaning outside that context (there's no "list all interviews across every application I've ever made" use case here, and no interview without a parent). The URL should say that: nesting makes the parent-child relationship visible in the route itself instead of leaving it implicit in a foreign key the client has to already know about. It also means every interview lookup is naturally scoped to its parent, which is what makes the ownership check below possible in the first place.

### `@ManyToOne` only, no `@OneToMany` back-reference

**Decision:** `Interview` has `@ManyToOne` to `JobApplication`. `JobApplication` has no `@OneToMany List<Interview>` pointing back.

**Alternative considered:** a bidirectional relationship, with `JobApplication` exposing its interviews directly.

**Why:** a `@OneToMany` collection on `JobApplication` invites exactly the two problems it's not worth taking on here. First, N+1 queries: fetching a list of applications and touching `.getInterviews()` on each one fires a separate query per application unless the fetch is deliberately tuned (join fetch, entity graph, batch size) every time, which is easy to forget and easy to regress. Second, lazy-loading exceptions: a lazy collection accessed outside its transaction (e.g. during JSON serialization, if the entity ever leaked that far) throws `LazyInitializationException`, and eager loading it instead just moves the N+1 problem earlier and makes it unconditional. Since every access pattern this API actually needs — "interviews for this application" — is already served by `InterviewRepository.findByApplicationIdOrderByScheduledAtDesc`, a query the interview side owns, there's no case where the back-reference would be used instead of that query. The relationship only needs to be navigable in the one direction it's actually used.

### Ownership validated via `findByIdAndApplicationId`, and the 404 is deliberately ambiguous

**Decision:** every interview lookup by id (`getInterviewById`, `updateInterview`, `deleteInterview`) goes through `findByIdAndApplicationId(id, applicationId)` (or `deleteByIdAndApplicationId`), not a plain `findById(id)`. If the interview exists but belongs to a different application, the response is the same 404 as if it didn't exist at all — never a 403 or an error message that says "wrong application."

**Why:** the two failure cases — "no interview with that id" and "that interview exists, but not under this application" — are handled by the same query and produce the same response on purpose. Returning a 403 or a differently-worded 404 for the second case would confirm to a caller that a given interview id exists somewhere, just not here — which is information leakage: it tells an attacker (or a caller fumbling ids) that the id is valid and worth trying against other application ids. A uniform 404 gives away nothing beyond "you don't have this," which is the same thing "this doesn't exist" tells them.

### `ON DELETE CASCADE`: interviews die with their application

**Decision:** the foreign key in `V2__create_interview_table.sql` is declared `ON DELETE CASCADE`. Deleting a `JobApplication` deletes its interviews at the database level, not via application code.

**Alternative considered:** delete interviews explicitly in `JobApplicationService` before deleting the application, or block the delete if interviews exist.

**Why:** an interview has no existence independent of its application — see the nested-routes decision above — so once the application is gone, an orphaned interview row isn't a resource, it's a dangling foreign key with nothing to attach to. `ON DELETE CASCADE` makes that invariant impossible to violate no matter which code path deletes the application (this service today, a future admin tool, a manual `DELETE` for a data fix), instead of relying on every caller to remember to clean up interviews first. Blocking the delete instead was rejected for the same reason `PUT` was made a full replacement: it would treat interviews as if they mattered independently of their application, which contradicts the whole reason they're nested.

### The initial result is `PENDING`, fixed by the factory method — same logic as `status`

**Decision:** `Interview.create(scheduledAt, type, application)` always sets `result` to `InterviewResult.PENDING`; there's no `result` field on `InterviewRequest` (create), only on `InterviewUpdateRequest`.

**Why:** this is the same rule as `JobApplication`'s initial `status`, applied to the same kind of field — see [The initial status is not accepted from the client](#the-initial-status-is-not-accepted-from-the-client). A newly scheduled interview hasn't happened yet, so `PENDING` isn't a default a caller could reasonably override at creation time, it's a fact about what "newly scheduled" means. Leaving `result` out of the create DTO enforces that the same way: there's no field to set it through, so a caller can't create an interview that's already `PASSED` or `CANCELLED` before it's taken place. Moving it to `PASSED`/`FAILED`/etc. is only meaningful once the interview exists, which is exactly what `InterviewUpdateRequest` is for.

## Scalability considerations

None of this is wired up yet, on purpose — it's sized for one user (me) hitting it from Postman, not production traffic. Here's specifically what's missing and why it matters:

- **No pagination.** `GET /api/v1/job-applications` loads and returns every row on every call. Fine at a handful of records; both response size and query cost grow linearly with the table, with no way for a caller to ask for "just page 3." Needs to become a `Pageable`-based query (page/size/sort as query params) before it could handle a real dataset.
- **No caching.** Every `GET` hits PostgreSQL directly, every time, even for data that hasn't changed between requests. For one user that's irrelevant; under real traffic the database becomes the bottleneck first, doing repeat work for identical reads that a read-through cache (in-process, or Redis if this ever runs as more than one instance) would absorb.
- **Only one index exists** (`idx_job_application_status`), and it's worth being precise about what that buys and what it doesn't. An index lets Postgres jump to matching rows instead of scanning the whole table. `idx_job_application_status` speeds up any query filtered by status (`WHERE status = 'INTERVIEWING'`), which is a reasonable bet since "show me my active applications" is a near-certain future query. It does nothing for other access patterns — sorting by `appliedOn`, or searching by `companyName`, still forces a full scan today, because no index covers them. That's not an oversight so much as a sequencing choice: every index speeds up reads on its column but slows down every insert/update (the index has to be maintained) and costs disk space, so adding indexes ahead of an actual query that needs them is guessing. Further indexes belong in the migration that ships the endpoint that actually queries by them.
- **No rate limiting.** Nothing stops a single caller — a buggy script, a retry loop, a scraper — from sending requests as fast as the network allows. Irrelevant while this only runs locally; the moment it's reachable from anywhere, one client can degrade or exhaust the service for everyone (or run up database load). This is a service-availability/fairness concern, not an auth concern, and is normally handled at the edge (gateway/reverse proxy) or in-app (e.g. Bucket4j, keyed by API key or IP).
- **No observability.** No structured logging, no metrics, no request tracing, no correlation ID tying a request to what happened downstream. Right now, if something breaks in production, the only signal is an external symptom (a failed request) with no way to find out why from the server side — no log line to grep, no metric to alert on, no trace to follow. Closing this gap — structured JSON logs, Spring Boot Actuator (health/metrics), and a per-request correlation ID — is planned for a later stage of the project, once the core API and its tests are solid.
