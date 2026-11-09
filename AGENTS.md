# AquaDesk — Architecture, Engineering, and AI Agent Guide

**Document version:** 2.0 **Purpose:** reference guide for AI coding agents and human contributors.

This document describes AquaDesk's known architecture, currently documented behavior, compatibility constraints, engineering conventions, operational boundaries, and expected agent workflow. It is written for use by different coding assistants. Each tool may have its own mechanism for discovering and loading repository instructions. The conventional filename `AGENTS.md` is used to make the guide easier to discover.

**Source-of-truth rule:** this document is an architectural guide, not proof that a particular implementation currently exists. Before changing code, inspect the relevant source files and tests. If the repository and this document disagree, report the discrepancy and determine the current behavior from the code before proceeding. Do not silently change the implementation to match outdated documentation. Update this document when a change intentionally alters an architectural rule or established contract.

**Current state versus target state:** behavior described as legacy, planned, recommended, or future must not be presented as already implemented. A proposal in this document does not, by itself, authorize a broad migration.

## 1. Product and scope

AquaDesk is a web application for managing multiple aquariums. It provides an event calendar and history, water parameter tracking, maintenance and filter cleaning schedules, CO₂ treatments and photos, fish and plant management, reminders, and notification settings, including Telegram notifications.

All functionality that manages aquarium data must preserve the aquarium context in its operations. The application must not treat the data as if there were only one aquarium.

## 2. Technology stack and repository structure

- Java 17 and Spring Boot 3.2.5.
- Maven multi-module project. Maven must be installed locally to build and test it.
- Spring Web MVC, Spring Data JPA, Jakarta Validation, and Thymeleaf.
- MySQL in the normal environment; in-memory H2 for application tests.
- Frontend without npm or a bundler: HTML with Thymeleaf, Bootstrap, FullCalendar, and vanilla JavaScript.
- SSE for notifying open calendars and other views about updates.
- Outbound integration with Telegram.

Main structure:

```text
.
├── app/
│   ├── src/main/java/romsanbryan/aquadesk/
│   │   ├── controllers/       # REST API and view routes
│   │   ├── services/          # Use cases and domain coordination
│   │   ├── dto/               # Input and output contracts
│   │   ├── mapper/            # Entity-to-DTO conversion
│   │   ├── evaluation/        # Parameter evaluation and extraction
│   │   ├── exception/         # Exceptions and HTTP translation
│   │   └── logging/           # Request tracing and logging
│   └── src/main/resources/
│       ├── templates/         # Thymeleaf views and fragments
│       ├── static/            # JavaScript, CSS, and images
│       ├── i18n/              # UI and backend messages
│       └── application.properties
├── data/
│   └── src/main/java/romsanbryan/data/
│       ├── entities/          # JPA entities
│       └── repositories/      # Spring Data interfaces
├── db/db.sql                 # Initial MySQL schema, no sample data
└── pom.xml
```

`AquadeskApplication` explicitly configures component scanning for
`romsanbryan.aquadesk`, `romsanbryan.parameters`, and
`romsanbryan.notifications`, in addition to scanning entities and repositories in the `data` module; it also enables scheduled tasks. If component, entity, or repository packages are added outside those packages, update `@ComponentScan`, `@EntityScan`, or `@EnableJpaRepositories`, respectively.

## 3. Module and layer boundaries

The dependency between modules is unidirectional:

```text
app  ─────►  data
```

- `data` contains JPA entities and repositories. It must not depend on `app` or contain presentation logic.
- `app` contains controllers, services, DTOs, mappers, and application logic.
- Controllers translate HTTP into service calls; they do not contain JPA queries or substantial domain logic.
- Services coordinate use cases and use repositories from the `data` module; they do not build JPQL queries.
- Repositories declare derived methods or JPQL queries. JPQL uses entity and Java attribute names, not SQL table or column names.
- Mappers are the place to convert entities to DTOs and vice versa.
- Do not return JPA entities from the API. Bidirectional relationships can cause serialization recursion and expose internal model details.
- Keep transactions at the use-case/service boundary and declare `readOnly = true` for transactional reads where appropriate.
- When a use case modifies multiple records that must remain consistent with one another, perform those modifications in a single transaction when the model and operation allow it. Do not split an atomic business operation into separate transactions without an explicit justification.
- Do not assume that `save()` alone means the entire transaction has committed. External side effects or notifications must be coordinated with the final transaction outcome; see section 8.

When adding functionality that requires persistence, review the change in this order: entity/repository in `data`, service, DTO and mapper in `app`, controller, frontend contract, schema, and tests.

## 4. Persistence and model compatibility

`db/db.sql` is the installation DDL for a new database: it contains the application's 13 tables, keys, indexes, relationships, and `CHECK` constraints, without inserting data. All tables specify `InnoDB` and `utf8mb4_0900_ai_ci`. The script does not create or select a database; users must select an empty database before running it. It is designed for MySQL 8.4 and must not be used as a migration on an existing installation.

When changing an entity, keep `db/db.sql` aligned with the model. The application configures Hibernate with `ddl-auto=update`, while tests use H2 with `create-drop`.

When an entity changes:

1. Review its type, nullability, length, precision, physical column name, keys, indexes, relationships, and deletion strategy.
2. Update `db/db.sql` in the same change and check consumers of the entity and repository.
3. Consider existing installations: editing the reference schema does not update an already deployed database by itself.
4. Do not run the schema, migrations, or destructive operations against a real database as part of a task or test.
5. Do not assume `ddl-auto=update` safely handles every schema change on existing installations. Before modifying the model, assess the impact on stored data and document any required manual steps.
6. Adopting a versioned migration tool (for example, Flyway or Liquibase) may be evaluated as a future evolution, but do not add, configure, or migrate the current schema without an explicit request and a reviewed transition plan.

Relationships between `Event` and its child entities (`Info`, `Parameters`,
`Co2`, `Maintenance`, and `Photo`) are backed by foreign keys with
`ON DELETE CASCADE` in the schema. Do not assume Java cascades beyond those declared by the model and database.

Photo binary content is stored in the database (`photoData` /
`photo_data`), not in the file system. Listings must return identifiers and metadata only; bytes are served through the image endpoint. When the last photo associated with an event is deleted, the event is also deleted, according to the current domain rule.

### Legacy names and contracts

The maintenance model uses English names in Java and in the API (`Maintenance`,
`water`, `primeDose`, `filters`). In the target schema, the tables and columns are named
`event_maintenances`, `water`, `prime_dose`, `glass_cleaned`, `event_id`, and
`maintenance_filters`; do not reintroduce the `profito` column. The stored event type
`manteiment` remains a legacy value and must not be changed incidentally.

Maintenance is managed at
`/api/aquariums/{aquariumId}/maintenances`. Request bodies also accept the old
`watter` alias and the `prime` alias. Some JSON fields in Spanish are retained for compatibility, including `fecha`, `nombre`, `valor`,
`ultimaFecha`, `proximaFecha`, and `diasRestantes`. Global Telegram sending retains
`POST /api/notifications/telegram/send` to avoid breaking external integrations
such as the cron job.

For filters, the JPA entity is still named `romsanbryan.data.entities.Filtre`
to match the legacy schema; the API DTO is `romsanbryan.aquadesk.dto.FilterDto`. Avoid reintroducing types with the same name across modules.

## 5. Domain: aquariums, events, and reminders

### Aquarium isolation

- Include `aquariumId` in routes that operate on aquarium resources:
  `/api/aquariums/{aquariumId}/<resource>`.
- Find, update, and delete each resource within the specified aquarium. A valid identifier belonging to another aquarium is treated the same as a nonexistent one and results in `ResourceNotFoundException` (404).
- Apply the aquarium filter in the repository; do not load a global resource and then decide whether it belongs to the aquarium.
- Prevent listings, calculations, notifications, and exports from mixing data across aquariums.
- Services operating on aquarium resources must require
  `aquariumId`; do not reintroduce variants without aquarium context. Intentionally global operations (for example, Telegram sending or the aggregated aquarium list) are explicit exceptions.
- Data isolation by `aquariumId` and access authorization are separate controls. Correctly filtering by aquarium does not, by itself, prove that the requesting user is allowed to access it.
- Before changing aquarium operations, inspect the security and identity model that actually exists. If the application has users, sessions, or permissions, verify that the requester is authorized for the aquarium for both reads and writes. Do not invent an authentication system or assume one exists unless the repository demonstrates it.
- Do not rely solely on an `aquariumId` received from the URL, form, or request body as proof of authorization.

### Events

The calendar unifies domain actions as events. In new code, reuse
`EventServices.createEvent(aquariumId, title, date, type)` to create the parent event and then associate the child entity by `idEvent`; do not duplicate event creation in controllers or services from other domains. If modifying a legacy route that still creates the event directly, migrate it carefully and cover its behavior with tests.

The event type is text and determines its icon/color through `EventMapper`. Existing types include `co2`, `info`, `live`, `manteiment`,
`parameters`, and `photo`. When adding a type:

- define its presentation in `EventMapper`;
- review the calendar and event groupings;
- include it in `HistoryExportService` if it should appear in an export;
- update translations and tests where appropriate.

Event titles that the API returns to the calendar may be HTML icon fragments, for example
`<i class='bi bi-...'></i>`. Generate that HTML only from application-controlled content; do not concatenate user observations or values into it.

Each `AquariumTask` retains an event type (`info`, `live`, `manteiment`,
`parameters`, `co2`, or `photo`). When completed, the event and corresponding
domain record are created and the task is deleted within a single transaction. The UI lets users review and edit event details before confirming; photos are sent as multipart data. The `event_type` column is added with a default of `info` to preserve existing tasks. On already deployed installations, review the actual schema update before enabling this version (if needed, apply
`ALTER TABLE aquarium_tasks ADD COLUMN event_type varchar(50) NOT NULL DEFAULT 'info'`).
`db/db.sql` must remain a complete and up-to-date DDL for the initial schema.

### Notifications

- `Notifications.weekdays` is a bit mask: Monday is bit 0 and `127`
  represents every day.
- A null value, `0`, or a mask with no valid days means the day is unrestricted; the interval is calculated using `value`.
- Calculate the next date relative to the most recent recorded event, not from today. For weekdays, start the search on the day after the last event.
- The `parameter` field supports prefixes: `co2:<product>` identifies a treatment; `info:<observation>` identifies an observation; without a prefix it can be interpreted as a legacy event type.
- A notification lookup must respect `aquariumId` at every step, including when resolving the latest event.

## 6. REST API and errors

### Conventions for new code

- Use English, plural routes organized by aquarium:
  `/api/aquariums/{aquariumId}/<resource>`. Aquarium listing and creation use
  `/api/aquariums`; filters, tasks, events, parameters, ranges, photos,
  maintenance, treatments, and notifications are nested under the aquarium.
- `aquariumId` is part of the route, not an `@RequestParam`.
- Define explicit request and response DTOs. Prefer `record` for immutable DTOs, following existing contracts; do not expose entities.
- Validate request bodies with `@Valid` and Jakarta annotations on the DTO. Also validate ranges and business rules in the service using the appropriate custom exception.
- API dates use ISO `yyyy-MM-dd`; use `@DateTimeFormat(iso =
  DateTimeFormat.ISO.DATE)` for date parameters.
- Controllers should return concrete types and let the global layer translate errors. Do not add controller-level `try/catch` or
  `ResponseEntity<?>`. A specialized HTTP response type, such as
  `ResponseEntity<byte[]>` for a download, may be necessary.
- Target status codes: `GET` 200; creation `POST` 201 with the created resource;
  `PUT` 200 with the updated resource; `DELETE` 204; actions with no content
  204. An empty collection is 200 with `[]`; a nonexistent resource is 404.
- After a mutation, publish the corresponding SSE update only after the change has successfully committed.
  `save()` or executing a statement does not by itself guarantee that the entire transaction has committed. Coordinate publication with the actual commit and do not emit events when the transaction rolls back. See section 8 as well.

### Error handling

Application exceptions live in `romsanbryan.aquadesk.exception`:

| Exception                    | HTTP | Usage                                            |
|------------------------------|-----:|--------------------------------------------------|
| `ValidationException`        |  400 | Invalid input or domain rule                    |
| `ResourceNotFoundException`  |  404 | Resource does not exist or belongs to another aquarium |
| `ServiceException`           |  500 | Controlled internal failure, preserving the cause |
| `ExternalServiceException`   |  502 | External integration failure                    |

All extend `AquadeskException`. `GlobalExceptionHandler` is the central REST error translation point and returns `ApiError`; it preserves the generic message `An unexpected error occurred.` for unexpected failures. API error messages are written in English and must not reveal internal details, credentials, or stack traces.

Services must let custom exceptions propagate and wrap unexpected repository or library failures in `ServiceException`, preserving the cause. Do not throw `RuntimeException`, `IllegalStateException`, or
`IllegalArgumentException` from services. When creating a new custom exception, also update the handler and `ExceptionAndLoggingTest`.

`GlobalExceptionHandler` is the only place that converts REST exceptions to `ApiError`. Controllers must not catch application exceptions to build responses; let them propagate to the global handler.

### Route contracts

Routes for aquarium-associated resources use `/api/aquariums/{aquariumId}/...`,
with English plural names. Global Telegram sending retains
`POST /api/notifications/telegram/send`; it is not tied to an individual aquarium.
When changing a contract, update the controller, `fetch` calls, and related tests together.

## 7. Frontend, views, and internationalization

### Views and JavaScript

- `templates/index.html` represents the `/acuario` list, served as a Thymeleaf
  skeleton; `static/aquarium-list.js` loads aquariums and their filters from
  `/api/aquariums`.
- `templates/gestion.html` represents aquarium management. It can receive
  `aquariumId` as navigation context and defines `window.i18n` before loading the management JavaScript.
- Thymeleaf should primarily serve the skeleton and translate template text. Do not add business data to the model beyond existing legacy exceptions.
- `static/gestion.js` is the entry point for `gestion.html`; it imports feature modules from `static/js/`.
  Place new code in its own files under `static/js/` and load them from
  `gestion.js` after defining `window.i18n`.
- Files in `static/` are not processed by Thymeleaf. Do not use `#{...}` expressions there.
- Keep historical JSON names consumed by the frontend until all their uses have also been migrated.
- Build FullCalendar dates with
  `getFullYear()/getMonth()/getDate()`, not `toISOString()`, to avoid date shifts caused by time zones.
- When changing date handling, determine whether the concept represents a calendar date (for example, a maintenance day) or an exact instant (for example, when an action occurred). Choose types and conversions consistently with the actual meaning; do not convert the two concepts interchangeably.
- When inserting user values into the DOM, use text-escaping APIs (`textContent`) and do not interpolate them into HTML. Calendar icon HTML is a controlled exception and must not contain arbitrary text.

Bootstrap 5.3.3, FullCalendar 6.1.11 (including locales and interaction), and Bootstrap Icons are loaded from a CDN with pinned versions in
`templates/fragments/static.html`. Do not introduce npm, a bundler, or update these versions as part of an unrelated functional task.

### Translations

- UI messages are in
  `app/src/main/resources/i18n/aquadesk.properties` (Spanish) and
  `aquadesk_en.properties` (English).
- All new UI text must exist in both languages.
- Templates can use `#{key}`; static JavaScript consumes
  `window.i18n.label.<key>`, declared in `gestion.html`.
- Backend-generated user-facing text (for example, Telegram messages or exports) is resolved through `MessageSource`.
- API error messages are in English and are not translated as UI text.
- Tests that depend on language must set `LocaleContextHolder` and restore it afterward.

## 8. SSE, integrations, and logging

`DataUpdatePublisher` publishes changes for the `/api/updates` SSE endpoint. Reuse existing topics rather than inventing incompatible names.

Publication must represent a committed change, not merely an attempted write. When the operation is managed by a transaction, prefer a mechanism that publishes after commit (for example, a
`@TransactionalEventListener` with phase `AFTER_COMMIT`) if it fits the existing implementation. Do not introduce this mechanism broadly without first reviewing the current flow. Verify in tests that publication does not happen when the operation fails or the transaction rolls back.

If AquaDesk later needs to guarantee that an integration event is not lost between the database commit and its publication, evaluate the Outbox pattern as an architectural evolution. Do not implement it for an ordinary functional task without justifying the need and reviewing its costs.

Telegram is a real external service. Tests must use
`MockRestServiceServer` and fake credentials; never make real HTTP calls from tests. In production code, convert errors from the external provider to `ExternalServiceException` and do not include tokens or sensitive data in logs or responses.

`RequestLoggingFilter` adds a trace identifier to the MDC and must clear it in `finally`; removing that cleanup can leak an identifier from one request to another. Do not log passwords, tokens, photo binary content, or sensitive request bodies. File logs must not contain ANSI codes.

## 9. Tests and commands

### Commands

Run these commands from the root so Maven compiles `data` together with `app`:

```bash
# Required standard suite before completing code changes
mvn -pl app -am test

# A specific test in the app module
mvn -pl app -am test \
  -Dtest=TestClassName \
  -Dsurefire.failIfNoSpecifiedTests=false

# Full reactor verification
mvn clean verify
```

Do not use `-pl app` without `-am` to validate changes that depend on `data`, because an older version installed locally could be resolved.

### Testing strategy

- Service tests: JUnit 5, Mockito, and `MockitoExtension`; mock repositories and external integrations, never a real database or real external services.
- Test success, missing resources or resources belonging to another aquarium, invalid input, persistence errors, and SSE publication. Verify SSE is published only after commit and is not published after rollback.
- For composite operations, also test atomicity: if part of the use case fails, no partial changes should remain persisted when the operation requires transactional consistency.
- Controllers: test the HTTP contract, validation, status codes, and error format.
- HTTP integration: centralize endpoint cases in
  `ApplicationEndpointIntegrationTest` with `@SpringBootTest`,
  `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, and transactions. The
  `test` profile uses isolated H2 and must not point to the real database.
- Telegram: use only a simulated HTTP server and fake data such as
  `test-token` and `chat-42`.
- Mappers, evaluation, exceptions, and logging: follow existing locations and patterns, including
  `MapperAndEvaluationTest` and
  `ExceptionAndLoggingTest`.
- Mockito is strict: remove unnecessary stubs; do not add speculative stubs.
- Name tests as behavior phrases in `camelCase`, for example
  `rejectsMissingOrBlankAquariumNames`.
- Do not weaken or delete tests to make them pass. Change assertions only when the contract behavior intentionally changes.

Documentation-only changes do not require running Maven unless a documentation-specific test exists.

## 10. Configuration, privacy, and operational boundaries

- Versioned configuration is in
  `app/src/main/resources/application.properties`; it contains configuration keys and must not receive real credentials.
- The personal override `application-local.properties` is ignored by Git:
  do not read, modify, create, or copy its values into other files.
- Keep credentials in local configuration or secure environment mechanisms. Never write, print, document, or commit real tokens, passwords, chat IDs, hosts, or database names.
- Before changing authentication, authorization, sessions, or CSRF protection, inspect the existing security configuration and its tests. Do not disable, weaken, or bypass security controls to make a test pass or facilitate an integration. Do not claim a control is implemented unless verified in the repository.
- For aquarium-related operations, separately verify isolation through `aquariumId` and, when a user context exists, that user's authorization for the aquarium. Apply the check on all relevant paths, including endpoints, exports, and scheduled tasks acting on behalf of a user.
- Do not connect to a real database, run SQL against it, or start the application unless explicitly requested. Startup may connect to the configured database.
- Do not call `POST /api/notifications/telegram/send` with real configuration:
  it sends an actual message.
- Do not deploy, publish images, push, or run commands against remote servers as part of normal development.
- Do not create commits or push unless explicitly requested.
- Do not add dependencies or change versions in `pom.xml` files unless explicitly requested.

## 11. Collaboration and change conventions

- Code, identifiers, comments, and commit messages must be in English.
- Keep changes surgical and limited to the requested resource and behavior.
- Before editing, review the current code and tests; do not assume outdated documentation precisely describes the current implementation.
- For a contract change, update all affected consumers and tests in the same change.
- Do not rename legacy components or concepts without an explicit request.
- Do not hide failures with default values, silent generic exceptions, or false success responses.
- Report the outcome of tests that were run; if they could not be run, say so explicitly.
- Do not present an inferred or unexecuted behavior as verified. Clearly distinguish what is verified, assumed, and pending.

## 12. Instruction priority and conflict resolution

When instructions, requirements, or documentation sources conflict, apply this priority order:

1. **Security, privacy, and data integrity.** Never expose secrets, bypass security controls, or perform destructive operations without explicit authorization.
2. **Explicit task requirements.** Deliver the result and respect the user's constraints unless they contradict a higher-priority rule.
3. **Existing public and persisted contracts.** Preserve API, database, JSON, frontend, and integration compatibility unless the task explicitly includes a controlled contract change.
4. **AquaDesk business rules.** Preserve aquarium isolation, event lifecycle, notification calculations, and other rules documented here and verified in the code.
5. **Existing architecture and engineering conventions.** Respect module boundaries, layer responsibilities, and naming and testing conventions.
6. **Agent implementation preferences.** Personal preferences, generic best practices, or a proposed refactor must not override the items above.

A lower-priority instruction must never silently override a higher-priority requirement. If a conflict cannot be resolved by consulting the task and repository, explain the alternatives and ask for clarification before making a decision that could affect data integrity, security, public contracts, or architecture. For small, reversible implementation decisions, choose the least surprising option consistent with the repository and report it.

## 13. AI agent workflow

AI agents must act as senior software engineers and architectural reviewers, not merely code generators. The following workflow applies to code changes.

### 13.1 Understand before changing

- Read relevant implementation, tests, consumers, and configuration before editing. Do not rely exclusively on this document, filenames, or assumptions based on common Spring Boot patterns.
- Trace the full affected flow where appropriate: HTTP route, controller, service, repository, entity, mapper, DTO, frontend consumer, persistence, and side effects.
- Search for uses of methods, fields, endpoint routes, and event types that will be changed.
- Identify existing behavior that must be preserved and legacy paths that share the affected code.

### 13.2 Verify and limit the scope

- Separate verified facts from assumptions. If a detail cannot be checked in the repository, state that it is unknown rather than inventing an implementation.
- Define the smallest set of files and components needed to fulfill the task.
- Identify possible impacts on data, API contracts, aquarium isolation, translations, SSE, integrations, and tests.
- For major or cross-cutting changes, provide a brief implementation plan before editing. Do not turn a narrowly scoped task into an unrequested general refactor.

### 13.3 Evaluate the proposed design

- Do not assume a requested implementation is architecturally correct just because the user asked for it. Evaluate it against the existing design, business rules, compatibility, and security boundaries.
- If a simpler or safer solution exists, explain its benefits and tradeoffs. Do not introduce incompatible changes or broad architectural migrations without explicit approval.
- Do not invent domain rules to fill information gaps. Consult existing code and tests; if the correct behavior remains ambiguous and materially affects the result, ask for clarification.

### 13.4 Implement incrementally

- Make the smallest coherent change that satisfies the task.
- Keep unrelated formatting, naming, dependency, and configuration changes out of the diff.
- Update all affected consumers and tests in the same change when a contract is intentionally modified.
- Do not introduce speculative abstractions or compatibility layers without a concrete need.

### 13.5 Validate and review

- Run focused tests first when useful, then the project's required suite for code changes, always respecting the operational restrictions in section 10.
- Review the final diff for accidental edits, exposed secrets, outdated comments, incomplete contract updates, and scope expansion.
- Check that tests cover relevant success and failure cases.
- Do not claim tests passed unless the command completed successfully. State relevant commands and summarize failures or omitted checks.

### 13.6 Report the outcome

Every completed code task should report, as applicable:

- What changed and why.
- The main affected files or components.
- API, data model, or behavior changes.
- Tests run and their actual results.
- Checks that could not be run and why.
- Known risks, assumptions, and pending work.

Never claim a deployment, database change, external send, commit, or push was performed unless explicitly requested and actually completed.

## 14. Engineering quality standards

Apply these principles pragmatically and consistently with the existing code. They are quality goals, not a justification for broad refactoring during an unrelated task.

### 14.1 Design and maintainability

- **Single responsibility:** keep classes and methods focused on one coherent responsibility. Split them when doing so improves clarity or testability.
- **DRY:** avoid duplicating non-trivial business rules or transformations. Do not force unrelated code into a shared abstraction just to remove a few repeated lines.
- **KISS:** prefer the simplest design that satisfies actual requirements.
- **SOLID:** respect responsibilities and dependency boundaries; use abstractions only when they improve evolvability, substitutability, or testability.
- **Explicit behavior:** avoid hidden side effects, surprising defaults, and ignored exceptions.
- **Names:** use descriptive English names for new code, consistent with project terminology.
- **Method and class size:** avoid accumulating unrelated behavior. Refactor only when relevant to the change and when tests can protect behavior.

### 14.2 Avoid overengineering

- Do not introduce interfaces, factories, strategy hierarchies, event buses, generic frameworks, additional modules, or new layers unless they solve a concrete problem in the existing architecture.
- Do not add dependencies or design patterns for hypothetical needs.
- Prefer existing utilities and conventions when they are suitable.
- Do not create a second source of truth for data or business rules.

### 14.3 Validation, errors, and observability

- Validate inputs at the appropriate boundary and enforce domain invariants in the service layer. Avoid duplicated and inconsistent validation rules.
- Preserve exception types and causes. Never catch an exception only to log and ignore it.
- Logs must provide useful operational context without logging secrets, credentials, sensitive request bodies, or unnecessary personal data.
- Avoid logging the same exception repeatedly at multiple layers unless each log adds meaningful context.

### 14.4 Performance and data access

- Avoid N+1 queries, unbounded collection loads, and repeated database calls inside loops when an appropriate query or batch load exists.
- Retrieve only the data needed for each use case when practical; do not load photo binary data in listings or metadata views.
- Consider pagination or result limits for collections that may grow. Do not introduce an incompatible pagination contract without a task that includes the required API and frontend changes.
- Add or modify indexes only after considering actual query predicates, cardinality, and write cost. Update the reference schema and tests where appropriate.
- Do not optimize based only on speculation. Identify the query or workload and verify the effect whenever possible.

## 15. Date, time, and time-zone policy

AquaDesk contains calendar events, maintenance schedules, treatments, and notifications. Date handling must preserve domain meaning and must not silently shift calendar days.

- Distinguish a **calendar date** from a **time instant**. Use existing conventions and appropriate Java types after inspecting the current model; do not treat `LocalDate`, `LocalDateTime`, and `Instant` as interchangeable.
- Determine whether a field represents a day selected by the user, a local date and time, or an absolute point in time before changing its type or serialization.
- When changing date fields, review the column type, JPA mapping, DTO format, JSON contract, JavaScript parsing, and calendar rendering together.
- Keep the existing API format (`yyyy-MM-dd`) for date-only contracts unless a coordinated contract migration is explicitly requested.
- Do not add a time zone or convert a date without a time to UTC unless its domain meaning requires it.
- For timestamps, use an explicit and consistent time-zone policy. Inspect current application configuration and persisted data before proposing changes; do not assume the server's time zone matches the user's.
- Preserve the FullCalendar local-date construction rule in section 7. Avoid `toISOString()` for calendar values that represent only a date when it could shift the day.
- Test edge cases where appropriate: midnight, month and year boundaries, leap years, daylight-saving transitions, and users or servers in different time zones.
- Notification calculations must continue to use the latest recorded event as their reference, as described in section 5. Do not replace that rule with a calculation from the current date.

This section establishes correctness requirements; it does not require migrating all existing date fields or changing the current time-zone configuration.

## 16. Data integrity, transactions, and external effects

- Treat a multi-step domain operation as a single use case. Identify which writes must commit or fail together and use an appropriate transaction boundary.
- Do not communicate success, publish SSE updates, or perform dependent actions before the operation reaches the required committed state.
- A database transaction does not automatically include external systems such as Telegram. Do not assume a database rollback can undo an external message that has already been sent.
- When an operation combines persistence and an external effect, review the required ordering and failure behavior. Prefer a design that avoids misleading or inconsistent state, and explain unavoidable partial-failure risks.
- Do not add automatic retries for non-idempotent operations unless duplicate effects are prevented and the retry policy is justified.
- Use idempotency or duplicate detection when an operation may be repeated and duplicate processing could harm data or user experience.
- Do not default to compensating actions, distributed transactions, or an Outbox implementation. Introduce them only when requirements justify their complexity and the design has been explicitly reviewed.

## 17. Scalability and future evolution

The current application is a personal multi-aquarium management system. Do not assume it needs distributed infrastructure or large-scale architecture. Keep future growth in mind without implementing it prematurely.

- Preserve strict aquarium isolation as data volume and the number of aquariums grow.
- Monitor the areas with the greatest potential growth: event history, calendar queries, parameter history, scheduled notifications, SSE connections, and photo storage.
- Keep photo metadata retrieval separate from binary-content retrieval, following the current endpoint behavior.
- Photos are currently stored in MySQL. External storage or object storage may be evaluated in the future if size, backups, database performance, or operational needs justify it. Do not move photos or change their contracts as part of an unrelated task.
- Consider bounded queries, appropriate indexes, pagination, and retention policies only after understanding product requirements and data-retention needs.
- Do not introduce microservices, message brokers, distributed caches, or additional infrastructure without a demonstrated need.

## 18. Definition of Done

A code change is complete only when all applicable criteria have been addressed. If any criterion cannot be met, state that explicitly instead of declaring the task fully complete.

- [ ] The implementation satisfies the requested behavior and acceptance criteria.
- [ ] Relevant code and tests were inspected before editing.
- [ ] Existing architecture, business rules, and aquarium isolation are preserved.
- [ ] API, JSON, database, and frontend contracts remain compatible or, if a change was requested, all affected consumers have been updated consistently.
- [ ] Validation, error handling, and authorization implications have been considered where applicable.
- [ ] Relevant success, validation, missing-resource, cross-aquarium resource, persistence, and failure cases are covered by tests.
- [ ] Transactional behavior and SSE or external effects have been verified where applicable.
- [ ] Spanish and English translations have been updated for new UI text.
- [ ] No credentials, private configuration, or sensitive data have been added to code, logs, tests, or documentation.
- [ ] No unrelated modified files or accidental formatting changes remain in the diff.
- [ ] The final diff has been reviewed.
- [ ] Relevant tests have been run and their actual results reported.
- [ ] Any unverified behavior, skipped test, limitation, or pending work has been clearly reported.

Documentation-only changes do not require running the Maven suite unless a documentation-specific check exists. All operational restrictions in section 10 still apply.

## 19. Documentation maintenance and evolution

- Keep this file focused on repository-wide instructions and architectural facts that agents need to know before changing code.
- When a rule becomes obsolete, update or remove it in the same change that intentionally replaces the behavior. Do not leave contradictory old and new rules.
- When a feature or migration changes the documented current state, update the relevant section and accurately describe any remaining legacy behavior.
- If the documentation grows substantially, consider moving detailed content to specialized files such as
  `docs/architecture/backend.md`,
  `docs/architecture/frontend.md`, `docs/architecture/database.md`,
  `docs/architecture/api.md`, `docs/architecture/integrations.md`, and
  `docs/development/testing.md`. If doing so, keep `AGENTS.md` as a concise entry point, specify exactly which files each agent should read for a given task, and avoid duplicated or conflicting rules.
- Do not create these additional files solely for organizational purposes while a single guide remains manageable.

## 20. Architectural decision principle

Act as a software architect and senior engineer, not merely a code generator. Understand the existing system before proposing changes. Preserve established architecture and business rules unless there is a justified reason to evolve them. Prioritize correctness, maintainability, security, backward compatibility, and simplicity over implementation speed. Challenge incorrect assumptions, identify potential regressions, and explain decisions and their architectural tradeoffs. Never invent existing functionality, claim unverified behavior, or introduce architectural changes silently. When information is missing, inspect the repository first; if an important decision cannot be resolved from the code, explain the uncertainty and ask for clarification before proceeding.
