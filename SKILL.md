# TRANSIT & TILE — Persistent Engineering Instructions

## 1. Project identity and objective

**Transit & Tile** is a fresher-level, full-stack hotel booking portal. It lets a visitor browse sample hotels, search by destination and travel dates, inspect one room category per hotel, book one room, receive a unique booking reference, retrieve a booking using that reference and the booking email, and cancel a confirmed booking.

The required surprise feature is **Destination Postcard Confirmation**: after a booking has been successfully persisted, present a visually distinctive, vintage-inspired destination postcard containing the real booking details returned by the backend. The weather shown is predefined illustrative sample content, not a live forecast. Generic destination copy must be used when no curated tip exists.

The six agreed screens are Home, Search Results, Hotel Details, Booking Details, Booking Confirmation Postcard, and My Bookings. Spin & Stay is postponed until the core MVP is complete and must not delay it. Authentication, payment integration, and an admin dashboard are out of scope.

The original assignment is the authority for acceptance criteria. Existing decisions recorded in this project conversation and `architecture.md` / `implementation_plan.md` are the implementation source of truth. If a conflict is discovered, stop and surface it rather than silently choosing a new design.

## 2. Mandatory technology stack

### Backend
- Java 17
- Spring Boot
- Spring Data JPA and Hibernate
- Maven
- REST APIs
- JUnit 5 and Mockito

### Frontend
- React
- Bootstrap 5
- Custom CSS
- React Router

### Database
- MySQL

Do not substitute frameworks, databases, build tools, or UI libraries. Do not add infrastructure or dependencies without explaining the need and obtaining approval. Use one consistent HTTP client approach in the frontend (prefer the browser Fetch API unless the project owner approves another dependency). Use the Spring Boot version compatible with Java 17 and record the selected version in the README when project setup begins.

## 3. Scope guardrails

### Required MVP
- At least five sample hotels.
- Hotel search by destination and check-in/check-out dates.
- Hotel details, amenities, room description, capacity, price per night, and availability.
- One room category per hotel is sufficient.
- Booking for one room, collecting full name, email, and phone.
- Backend-calculated total and clear price breakdown.
- Unique booking reference.
- Booking lookup using reference plus email.
- Cancellation using reference plus email.
- Availability released when a booking is cancelled.
- Input validation, useful errors, automated tests, README, DESIGN.md, screenshots/demo evidence.

### Explicitly out of scope unless separately approved
- User accounts/login, payment processing, admin dashboard, live weather integration, multi-room/multi-category selection, reward wheel, deployment infrastructure, and production hotel-provider integrations.

Do not add features merely because they are common in commercial booking systems.

## 4. Coding conventions

### General
- Prefer clear, beginner-explainable code over clever abstractions.
- Use descriptive names and small methods with one responsibility.
- Avoid duplicated business rules. Availability and price calculation must have one authoritative backend implementation.
- Do not use magic numbers or unexplained strings; name meaningful constants.
- Do not swallow exceptions, log secrets, or return stack traces to clients.
- Keep comments focused on why a non-obvious decision exists, not on restating each line.
- Avoid unnecessary dependencies and premature generic frameworks.

### Java
- Use standard Java naming: `PascalCase` for classes, `camelCase` for fields/methods, uppercase snake case for constants.
- Use packages under `com.transitandtile`.
- Use `BigDecimal` for money; never use `float` or `double` for monetary calculations.
- Use `LocalDate` for check-in/check-out and `LocalDateTime`/`Instant` for audit timestamps as appropriate.
- Use constructor injection; do not use field injection.
- Keep controllers thin. Controllers parse HTTP input, invoke services, and return DTOs.
- Put business rules in services, persistence queries in repositories, and persistence mapping in entities.
- Use request/response DTOs. Never serialize JPA entities directly.
- Use explicit validation annotations and service-level checks for business invariants.
- Use transactions at service boundaries. Do not put transaction orchestration in controllers.
- Use enums for booking status; permitted values are `CONFIRMED` and `CANCELLED`.

### React
- Use function components and hooks.
- Use React Router for navigation.
- Keep API calls in `src/services`; do not scatter endpoint strings and fetch logic through page components.
- Keep reusable UI in `components` and route-level screens in `pages`.
- Use Bootstrap 5 for grid, layout, forms, cards, alerts, buttons, and modal primitives where appropriate; use custom CSS for the sky-blue/white brand and postcard treatment.
- Keep form values controlled and show field-level validation and API errors.
- Disable duplicate submission while a request is pending.
- Never calculate or submit a client-authoritative booking total.
- Never put customer email or other personal data in URL query strings or route paths.
- Do not render success/postcard state until the backend confirms the booking.
- Every visible button must perform its advertised action; remove or clearly disable unfinished actions.

## 5. Architecture and separation of concerns

Backend flow:

`HTTP Controller → Request DTO validation → Service/business rules → Repository → MySQL`

Response flow:

`MySQL/entity → Service → Response DTO → Controller → JSON`

- `controller`: REST boundary only.
- `service`: booking, search, availability, reference generation, and orchestration.
- `repository`: Spring Data JPA persistence and focused queries.
- `entity`: database-mapped domain state.
- `dto/request` and `dto/response`: API contracts.
- `exception`: domain exceptions and centralized error mapping.
- `config`: narrowly scoped configuration only.

Frontend flow:

`Page → reusable components + API service → REST API`

Do not place business-critical booking rules solely in the frontend. The backend is authoritative for date validation, availability, total amount, reference generation, booking status, and cancellation.

## 6. Database and JPA rules

The agreed core schema has exactly three domain tables:
- `HOTEL`
- `ROOM_TYPE`
- `BOOKING`

Relationship:
- One `HOTEL` has one `ROOM_TYPE` for this assignment (enforce unique `hotel_id` in `ROOM_TYPE`).
- One `ROOM_TYPE` has many `BOOKING` records.
- A booking references one room type.

Use InnoDB. Use primary keys, foreign keys, `NOT NULL`, unique constraints, sensible string lengths, and indexes. Use `DECIMAL(10,2)` (or a justified larger precision) for prices and booking totals. Use `DATE` for stay dates and a timestamp for creation time. Use a database unique constraint on `booking_reference`.

Recommended constraints:
- `ROOM_TYPE.hotel_id` unique and a foreign key to `HOTEL`.
- `BOOKING.room_type_id` foreign key to `ROOM_TYPE`.
- `check_out > check_in`.
- `price_per_night > 0`, `total_rooms > 0`, and `capacity > 0`.
- `total_amount >= 0`.
- `booking_reference` unique and non-null.
- `status` restricted by application enum and, where practical, a database check constraint.

Use explicit JPA relationship ownership and fetch behavior. Avoid eager-loading entire object graphs. Avoid cascading deletes from hotels/rooms to bookings. Do not delete historical bookings as part of cancellation.

Availability indexes should support filtering by `room_type_id`, `status`, `check_in`, and `check_out`. Validate the actual query plan against MySQL rather than assuming a composite index is optimal.

## 7. Booking business rules — authoritative

### Date rules
- Check-in is required and cannot be before the current date in the application’s agreed local date context.
- Check-out is required and must be strictly after check-in.
- A stay is measured in nights using `ChronoUnit.DAYS.between(checkIn, checkOut)`.
- Same-day check-out and next guest check-in do not overlap.

### Overlap rule
Two stays overlap when:

`existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn`

Only `CONFIRMED` bookings occupy rooms. `CANCELLED` bookings never count toward occupancy.

### Availability
For a room type and requested date range:

`availableRooms = totalRooms - count(overlapping CONFIRMED bookings)`

A request is bookable only when `availableRooms >= 1`, because the MVP books exactly one room. Never rely on a frontend availability label as proof that a room remains available. Recheck availability inside the booking transaction immediately before saving.

### Booking price
- The client sends hotel/room identifier, dates, and customer details only.
- The backend loads the room price from MySQL.
- `nights = days between check-in and check-out`.
- `total = pricePerNight × nights`.
- Use `BigDecimal` with an explicit currency rounding policy (normally scale 2, `RoundingMode.HALF_UP` where rounding is needed).
- Do not accept `totalAmount`, price, booking reference, or status as authoritative client input.
- Recommended audit improvement: persist `price_per_night_at_booking` in `BOOKING`, so later room-price edits do not rewrite the historical booking’s economics. This is an additional column in the agreed `BOOKING` table, not a fourth table; record the decision in DESIGN.md and get owner approval before implementing it.

### Booking reference
- Generate on the backend only.
- Use a readable format such as `TT-GOA-4891`; the suffix must have enough entropy for collision resistance.
- The database unique constraint is the final authority.
- If a collision occurs, retry generation a small bounded number of times; do not assume a random format guarantees uniqueness.
- Avoid deriving the destination code from untrusted arbitrary text without normalization and fallback behavior.

### Retrieval and cancellation
- Lookup requires both booking reference and email; compare normalized email consistently.
- Do not reveal booking details when the email does not match.
- Cancellation requires both reference and email, only changes `CONFIRMED` to `CANCELLED`, and does not delete the row.
- An already-cancelled booking cannot be cancelled again; return a clear conflict/state response.
- Availability is released automatically because availability counts only confirmed bookings.
- Do not expose an endpoint that lists all customers’ bookings; there is no authentication.

## 8. Transactions and concurrency protection

A plain “check availability, then insert booking” sequence is unsafe: two simultaneous requests can both observe the last room as available.

For this MVP, use a service-level transaction and acquire a pessimistic write lock on the relevant `ROOM_TYPE` row before counting overlapping confirmed bookings. Then, within the same transaction:
1. Lock the room type row.
2. Read its inventory and price.
3. Recalculate overlapping confirmed bookings.
4. Reject with a conflict if no room remains.
5. Calculate the total from the database price.
6. Generate a reference.
7. Persist the booking.
8. Commit.

All booking creation paths must acquire the same lock in the same order. Keep the transaction short; do not perform network calls while holding the lock. Confirm that the lock query is executed against MySQL/InnoDB and add an integration test for concurrent attempts if feasible. Unit tests alone cannot prove database locking works.

Cancellation should also be transactional. It should lock or otherwise safely serialize updates for the relevant room type/booking as appropriate, verify reference and email, and change status once. Define and consistently follow lock ordering to reduce deadlock risk.

A pessimistic lock serializes booking attempts for the same room type and is a practical small-project solution; it can reduce throughput. Document this trade-off. Do not claim the implementation is race-proof until an integration test exercises the actual MySQL behavior.

## 9. API conventions

- Use `/api` prefix and JSON.
- Validate request DTOs at the controller boundary and repeat critical business invariants in services.
- Use stable, documented response shapes.
- Do not expose entity objects.
- Use `400` for malformed/invalid input, `404` for unavailable resource or non-matching booking lookup, `409` for no remaining inventory or invalid state transition, and `500` for unexpected server errors.
- Return a consistent error body with timestamp, status, machine-readable code, message, and optional field errors. Never include stack traces or SQL details.
- Booking lookup and cancellation send email in the request body, not the URL.
- Document date format as ISO `YYYY-MM-DD`.
- Do not claim that an endpoint has been implemented until it exists and has been tested.

## 10. Validation and security

- Validate nonblank customer name, valid email, phone format/length, positive room identifier, and valid date range.
- Normalize email (trim and lowercase) before persistence and comparison.
- Set reasonable maximum lengths for user text.
- Treat all client input as untrusted.
- Use parameterized repository queries; never concatenate user input into SQL.
- Configure CORS narrowly for the local frontend origin during development.
- Keep credentials in environment variables or a local untracked environment file. Provide a safe example file with placeholders only if needed; never commit real credentials.
- Add `application-local`/environment configuration only when required; do not put secrets in committed defaults.
- Avoid logging customer email, phone, or full booking payloads unnecessarily.
- No authentication is required, so booking lookup/cancellation must always verify both reference and email and must return no booking data on mismatch. This is a limited verification mechanism, not strong identity authentication; document the residual risk.

## 11. Testing standards

### Backend unit tests (JUnit 5 + Mockito)
Test:
- Night calculation and total calculation, including one-night and multi-night stays.
- Invalid dates and missing customer data.
- Overlap boundaries (checkout equals next check-in is not overlap).
- Overlapping stays are counted.
- Cancelled bookings are excluded.
- Sold-out behavior.
- Booking service rejects when inventory is exhausted.
- Reference collision retry behavior where practical.
- Lookup requires matching email.
- Cancellation only transitions a confirmed booking once.

### Backend integration tests
Where practical, use a real MySQL test database (or an approved isolated MySQL test setup) for:
- JPA mappings and constraints.
- Repository overlap query.
- Pessimistic lock behavior and concurrent booking attempts.
- Transaction rollback on failed booking.

Do not imply an H2 test proves MySQL/InnoDB locking semantics. If an alternate test database is used for fast tests, explicitly state its limitations.

### Frontend tests / manual verification
At minimum verify:
- Search field and date validation.
- Empty results and API failure states.
- Loading and duplicate-submit behavior.
- Booking summary matches backend response.
- Confirmation appears only after success.
- Booking lookup mismatch and cancellation flow.
- Responsive layout and every visible action.

Record actual test commands and results. Never report tests as passing unless they were run.

## 12. Documentation and Git standards

The repository must include:
- `README.md`: prerequisites, Java/Maven/Node/MySQL versions, environment setup, database setup, run instructions, test instructions, sample data, and known limitations.
- `DESIGN.md`: architecture, schema/relationships/constraints, API contracts, booking/availability logic, concurrency strategy, assumptions, surprise feature, trade-offs, limitations, and future improvements.
- SQL schema/seed scripts or migrations only when implementation begins and after the owner approves the chosen setup approach.
- Screenshots or demo evidence after the UI exists.
- `.gitignore` covering build outputs, node modules, IDE files, logs, and local secret files.
- No secrets, customer data, or local database dumps in Git.

Make commits small and descriptive. Do not claim the workspace is a Git repository or remote repository until Git has actually been initialized/configured.

## 13. AI-assisted development protocol

At the beginning of each implementation session:
1. Read this `SKILL.md`, `architecture.md`, and `implementation_plan.md`.
2. Inspect the current workspace before editing.
3. Identify the current approved phase and its acceptance criteria.
4. Make only the changes required for that phase.
5. Do not regenerate or overwrite working files without inspecting them.
6. Explain important code in language the project owner can understand and defend in an interview.
7. Run relevant tests/builds and report exact outcomes.
8. Update documentation and the phase checklist to reflect reality.
9. Separate implemented, tested, untested, and deferred items.

Never:
- Start a later phase without approval or completion of dependencies.
- silently change the stack, schema, endpoints, business rules, or screen decisions.
- add authentication, payment, admin, reward wheel, live weather, or extra tables without approval.
- create fake UI actions, fake API responses, or claim success before backend confirmation.
- mark a task complete merely because files were generated.

If a technical weakness is discovered, document the issue, its impact, a practical recommendation, and whether owner approval is required before changing the plan.

## 14. Definition of engineering quality

The result should be a small, coherent, working MVP whose behavior is correct and explainable. Prioritize correctness of date overlap, availability, server-side pricing, cancellation, and concurrency over decorative features. Keep the implementation understandable for a junior developer and defend every major decision in the interview.
