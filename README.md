# Field Dispatch Operations

A reactive Spring WebFlux + Thymeleaf logistics/field-ops dispatch application.
Dispatchers assign jobs to field workers, workers update job status and upload
proof of completion, customers track their jobs, and ops admins manage
regions, users, service types, imports/exports, webhooks, and the audit log.

## Stack

- Java 21, Spring Boot 3.3, Spring WebFlux (reactive), reactive Thymeleaf views
- Spring Data R2DBC against PostgreSQL (bound parameters only)
- Spring Security (session-cookie auth, CSRF enabled, BCrypt password hashing)
- Flyway migrations, Gradle wrapper (`./gradlew`)
- Local mocks only: outbound email -> `outbox_emails` table, webhooks -> local
  `/api/mock/webhook-sink` endpoint with delivery-attempt logging

## Tier / trust statement

This app is a single deployable module (per the app spec), so the corpus-wide
`channel` / `decision` split is collapsed into one Spring Boot process:

- **Channel** responsibilities (authentication, session handling, CSRF,
  rendering) are handled by Spring Security's session-cookie login and the
  WebFlux/Thymeleaf controllers.
- **Decision** responsibilities (authorization) are owned by
  `AuthorizationService` and the domain services that call it. Every
  protected route resolves the acting user's id/role/region **from the
  authenticated principal loaded from our own database on each request**
  (`CurrentUser` + `AppUserPrincipal`), never from a client-supplied id, role,
  or region parameter. Region scoping for dispatchers is loaded from the
  `dispatcher_regions` table server-side.

## Precision baseline (5 required primitives - not vulnerabilities)

1. **Ownership/scope check one frame above the controller**:
   `AuthorizationService.assertCanAccessJob` / `assertDispatcherRegion` are
   called by `JobService`, `ProofService`, `IncidentService`, etc. - never
   inlined in a controller.
2. **`java.util.Random`**: `util/AssetVersion.java` uses it only to compute a
   cache-busting suffix for static asset URLs. Explicitly commented as
   non-cryptographic; never used for tokens or session ids (those use
   `SecureRandom`, see `UserService`).
3. **MD5**: `service/ReferenceDataService.java` uses MD5 only to compute an
   HTTP ETag fingerprint for the read-only `/api/reference-data` response.
   Commented as non-cryptographic; not used for passwords or signatures.
4. **Whitelisted sort/filter column**: `service/JobQueryService.java` builds
   the dispatch board query with bound parameters for every value; the sort
   column name is resolved through a fixed whitelist map before being
   concatenated into the SQL string (never through raw client input).
5. **Canonicalized file downloads**: `service/FileStorageService.java`
   resolves every proof-document download against the canonicalized upload
   base directory and rejects any path that would resolve outside it.

## Roles and demo credentials

All demo accounts use the password **`Password123!`**.

| Role | Email |
|---|---|
| Ops admin | `admin@fielddispatch.io` |
| Dispatcher | `dispatcher1@fielddispatch.io` (also `dispatcher2-4@...`) |
| Field worker | `worker1@fielddispatch.io` (also `worker2-10@...`) |
| Customer | `customer1@fielddispatch.io` (also `customer2-12@...`) |

## Running locally

### One-command setup (Docker)

```bash
docker compose up --build
```

This starts PostgreSQL, runs Flyway migrations, loads seed data, and starts
the app at **http://localhost:8471** (mapped from the container's port 8080;
Postgres is exposed on host port 55461 to avoid local port clashes).

### Running without Docker

```bash
# 1. Start Postgres (or point at your own instance)
docker run --rm -d --name fielddispatch-pg -e POSTGRES_DB=fielddispatch \
  -e POSTGRES_USER=fielddispatch -e POSTGRES_PASSWORD=fielddispatch_local_dev \
  -p 55461:5432 postgres:16-alpine

# 2. Run the app (Flyway migrates + seeds automatically on boot)
DB_HOST=localhost DB_PORT=55461 DB_NAME=fielddispatch \
DB_USER=fielddispatch DB_PASSWORD=fielddispatch_local_dev \
./gradlew bootRun
```

The app listens on port 8080 by default (override with `SERVER_PORT`).

### Tests

```bash
./gradlew test
```

Includes workflow-rule tests (`JobStatus` transition edge list) and a
security test verifying the canonicalized-path file download rejects
traversal outside the upload base directory.

### Build

```bash
./gradlew build
```

## Seed data

- 4 regions, 1 ops admin, 4 dispatchers (region-scoped via
  `dispatcher_regions`), 10 field workers, 12 customers with 1-2 service
  locations each, 6 service types (some requiring proof before completion)
- 44 jobs spanning every status (`unassigned`, `assigned`, `en_route`,
  `arrived`, `in_progress`, `completed`, `cancelled`, `failed`), including
  rescheduled jobs
- 14 proof uploads (mixed `CUSTOMER_VISIBLE` / `INTERNAL` visibility, all 4
  categories) with matching placeholder files under `uploads/`
- 9 incidents across all severities/statuses, with internal notes vs.
  customer-visible summaries
- 4 daily routes with ordered stops, 1 seeded webhook subscription, job
  messages, notifications, and one audit-log bootstrap entry (all further
  audit rows are produced by real actions during use)

## Key screens

- **Dispatcher board** (`/dispatch/board`): filter by status/priority/worker,
  assign jobs, create new jobs, region-scoped summary cards
- **Job detail** (`/jobs/{id}`): customer, location, schedule, worker, proof
  documents, incidents (internal notes hidden from customers), messages,
  role-appropriate action forms
- **Field worker dashboard** (`/worker/dashboard`): today's route order,
  assigned jobs, status updates, proof upload
- **Customer portal** (`/customer/portal`): only the customer's own jobs
- **Incident queue** (`/incidents`): staff-only triage/assignment/resolution
- **Admin** (`/admin/*`): regions, workers, service types, users (role
  changes / enable-disable), CSV imports, CSV exports (completion report,
  incident report, route schedules), webhooks + delivery log, append-only
  audit log

## Security controls implemented

- Session-cookie auth via Spring Security, CSRF enabled on every state-
  changing form (auto-injected hidden token via
  thymeleaf-extras-springsecurity6)
- Explicit DTOs for all form binding - no mass assignment of `role`,
  `enabled`, or ownership fields
- BCrypt password hashing; single-use, time-bound password reset tokens
  (`SecureRandom`-backed) recorded to `password_reset_tokens`
- Server-side authorization on every protected route/record/download/export/
  admin action, resolved from the session-loaded principal
- Parameterized queries throughout (R2DBC bind parameters); the one
  interpolated SQL fragment (sort column) is restricted to a fixed whitelist
- Default Thymeleaf escaping everywhere - no `th:utext` on user content
- Append-only audit log: no code path issues `UPDATE`/`DELETE` against
  `audit_log`

## Known limitations / not completed

- Everything in the app-specific and shared-core specs is implemented and
  exercised end-to-end (see verification above). The one intentional
  simplification: `docker-compose.yml` maps host ports 8471/55461 instead of
  the default 8080/5432 to avoid collisions with other locally-running
  benchmark apps; adjust freely for a standalone deployment.
