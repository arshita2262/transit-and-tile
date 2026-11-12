# TRANSIT & TILE — Phased Implementation Plan

**Status:** Planning checklist only. No application source code has been created.  
**Rule:** Complete and verify a phase before moving to the next. The project owner must approve the foundation before Phase 1 begins.

## Scope and delivery priorities

### Essential MVP
- Five or more sample hotels.
- Search by destination and dates.
- Hotel details and one room category per hotel.
- Correct date-based room availability.
- One-room booking with name, email, and phone.
- Server-side total calculation and unique booking reference.
- Lookup using reference + email.
- Cancellation using reference + email; cancelled booking releases inventory.
- Clear validation and error responses.
- Destination Postcard Confirmation using confirmed booking data.
- Automated tests for price calculation and availability, plus meaningful workflow tests.
- README, DESIGN.md, sample data/setup instructions, and screenshots/demo evidence.

### Deferred / optional
- Spin & Stay reward wheel.
- Live weather.
- Authentication, payment, admin dashboard, multi-room booking, production hotel integrations, and deployment.
- Any additional framework, database, service, or domain table.

## Development sequence

The sequence below preserves the agreed order: backend setup and core booking APIs first, then React UI, integration, postcard, and final verification.

---

## Phase 0 — Engineering foundation (current task)

**Objective:** Establish the project workspace and durable technical instructions before application coding.

**Tasks**
- [x] Finalize project scope and technology stack from the project discussion.
- [ ] Create root folder and backend/frontend directory skeleton.
- [ ] Create `SKILL.md`.
- [ ] Create `architecture.md`.
- [ ] Create `implementation_plan.md`.
- [ ] Review cross-document consistency.
- [ ] Obtain explicit project-owner approval.

**Files/directories**
- Root: `SKILL.md`, `architecture.md`, `implementation_plan.md`.
- Backend package/resource/test directories.
- Frontend `public` and `src` directories.
- `docs/` directory.

**Dependencies:** Existing project discussion and assignment requirements.

**Deliverables:** Verified workspace tree and three reviewed foundation documents.

**Testing / verification**
- Verify all three Markdown files exist and are non-empty.
- Verify intended directories exist.
- Confirm no application source, SQL, migrations, or MCP configuration was generated.
- Cross-check stack, three-table schema, endpoint list, date overlap, status, and screen list across documents.

**Completion criteria**
- Owner has reviewed the artifacts and explicitly approved Phase 1.
- No application code is written before approval.

---

## Phase 1 — Project setup and MySQL configuration

**Objective:** Create runnable but minimal Spring Boot and React project foundations and connect the backend to MySQL.

**Tasks**
- [ ] Confirm Java 17 and Maven availability.
- [ ] Select a Spring Boot version compatible with Java 17; record it.
- [ ] Generate backend Maven project with only necessary dependencies: Spring Web, Spring Data JPA, MySQL driver, validation, and test dependencies (JUnit 5/Mockito through Spring Boot test support).
- [ ] Create React project using a compatible, maintained setup; retain React as the agreed framework.
- [ ] Install/configure Bootstrap 5 and React Router.
- [ ] Configure API base URL for local development.
- [ ] Create MySQL schema/database manually or using the approved local setup method.
- [ ] Configure datasource through environment variables; provide a placeholder example only if useful.
- [ ] Add `.gitignore` for Maven target, node_modules, IDE files, logs, and local secret files.
- [ ] Add initial `README.md` with prerequisites and startup instructions.
- [ ] Confirm backend starts and can connect to MySQL; confirm frontend starts.

**Files/modules to create**
- Backend: `pom.xml`, application entry point, `src/main/resources/application.properties` (no secrets).
- Frontend: `package.json`, `public/`, `src/App.jsx`, `src/index.js` and only the minimal bootstrap entry files required to run.
- Root: `.gitignore`, initial `README.md`.
- Do not create business controllers/services/entities in this phase.

**Dependencies:** Phase 0 approval; Java 17, Maven, Node/npm, and MySQL installed locally.

**Deliverables**
- Backend launches.
- Frontend launches.
- Backend connects to local MySQL using environment-supplied credentials.
- Reproducible setup instructions.

**Testing**
- `mvn test` / Maven build succeeds.
- Frontend development server starts and renders the minimal app.
- Confirm invalid/missing DB credentials fail clearly without leaking secrets.
- Confirm no real credentials are committed.

**Completion criteria**
- Both development servers start.
- MySQL connection is verified.
- README setup instructions work on the project machine.

---

## Phase 2 — Database entities, repositories, and sample hotel data

**Objective:** Implement the three-table persistence model and seed at least five hotels.

**Tasks**
- [ ] Implement `Hotel`, `RoomType`, and `Booking` entities.
- [ ] Implement `BookingStatus` enum with only `CONFIRMED` and `CANCELLED`.
- [ ] Map Hotel 1—1 RoomType and RoomType 1—many Booking correctly.
- [ ] Add constraints and indexes.
- [ ] Use `BigDecimal`, `LocalDate`, and timestamp types appropriately.
- [ ] Create repositories for hotels, room types, and bookings.
- [ ] Add sample data for at least five fictional/demo hotels, each with one room category.
- [ ] Use sample/demo ratings and clearly identify them as such.
- [ ] Decide the approved representation for amenities and image arrays (e.g. MySQL JSON).
- [ ] Obtain owner decision on recommended `price_per_night_at_booking` column before finalizing Booking schema.

**Files/modules to create**
- `entity/Hotel.java`
- `entity/RoomType.java`
- `entity/Booking.java`
- `entity/BookingStatus.java`
- `repository/HotelRepository.java`
- `repository/RoomTypeRepository.java`
- `repository/BookingRepository.java`
- Database schema/seed setup using the approved method (not before the setup method is chosen).
- Repository tests under `src/test`.

**Dependencies:** Phase 1 working backend and MySQL.

**Deliverables**
- Three tables created with correct keys and relationships.
- At least five sample hotels and room records.
- Repository layer capable of loading the sample data.

**Testing**
- Verify JPA schema/mappings against MySQL.
- Verify unique hotel-to-room-type relationship.
- Verify foreign keys and positive-value/date constraints where supported.
- Verify seed data loads once without accidental duplicates.
- Verify bookings are retained and not cascade-deleted.

**Completion criteria**
- MySQL contains the agreed three-table model.
- Sample hotel data can be retrieved through repositories.
- Schema and sample data are documented.

---

## Phase 3 — Hotel search, hotel details, and availability APIs

**Objective:** Expose hotel data and correct date-specific availability through REST APIs.

**Tasks**
- [ ] Define request/response DTOs for hotel search, hotel cards, hotel details, and availability.
- [ ] Implement `HotelService` and `AvailabilityService`.
- [ ] Implement `HotelController`.
- [ ] Implement destination search with validated location and date range.
- [ ] Implement overlap query using `existing.checkIn < requested.checkOut AND existing.checkOut > requested.checkIn`.
- [ ] Count only `CONFIRMED` bookings.
- [ ] Calculate `availableRooms = totalRooms - overlappingConfirmedBookings`.
- [ ] Return availability label `Available` or `Sold Out` for the one-room booking flow.
- [ ] Return empty result list for valid searches with no matching hotels.
- [ ] Return details including description, amenities, room category, capacity, price/night, images, and policies.
- [ ] Add centralized validation/error response handling.

**Files/modules to create**
- `controller/HotelController.java`
- `service/HotelService.java`
- `service/AvailabilityService.java`
- DTOs under `dto/request` and `dto/response`
- Availability and hotel repository query methods.
- Domain exceptions and global exception handler under `exception/`.

**Dependencies:** Phase 2 entities and repositories.

**Deliverables**
- `GET /api/hotels`
- `GET /api/hotels/{hotelId}`
- `GET /api/hotels/{hotelId}/availability`
- Stable DTO contracts and documented error shape.

**Testing**
- Valid search and no-result search.
- Invalid/missing location and dates.
- Check-out must be after check-in; check-in cannot be in the past.
- Overlap boundary: one booking checking out on the requested check-in date does not overlap.
- Confirmed overlapping booking reduces availability.
- Cancelled overlapping booking does not reduce availability.
- Sold-out and available outcomes.
- Unknown hotel returns correct status.

**Completion criteria**
- All three read APIs work through actual MySQL data.
- Availability tests pass for boundaries and cancelled bookings.
- API request/response examples are recorded in DESIGN.md.

---

## Phase 4 — Booking creation, pricing, validation, and concurrency protection

**Objective:** Implement correct, transactional one-room booking creation.

**Tasks**
- [ ] Define booking creation request and response DTOs.
- [ ] Validate room type ID, dates, customer name, email, and phone.
- [ ] Normalize email consistently.
- [ ] Implement night calculation using date difference.
- [ ] Load current room price from MySQL.
- [ ] Calculate total with `BigDecimal`; never accept client total/price as authoritative.
- [ ] Generate readable `TT-DEST-XXXX`-style reference server-side.
- [ ] Add unique DB constraint and bounded retry on reference collision.
- [ ] Implement transaction boundary in `BookingService`.
- [ ] Acquire pessimistic write lock on the relevant RoomType row.
- [ ] Recalculate overlapping confirmed bookings after acquiring the lock.
- [ ] Reject exhausted inventory with `409 Conflict`.
- [ ] Persist `CONFIRMED` booking only after checks succeed.
- [ ] Return complete booking DTO with backend-calculated amount.
- [ ] Keep transaction short; do not make external calls while locked.

**Files/modules to create/update**
- `controller/BookingController.java` (booking create endpoint only in this phase)
- `service/BookingService.java`
- `service/BookingReferenceGenerator.java`
- Booking request/response DTOs
- Locked room type repository method and overlap count query
- Domain exceptions and error mapping
- Booking service and repository tests

**Dependencies:** Phase 3 availability logic and DTO/error conventions.

**Deliverables**
- `POST /api/bookings`
- Correct server-side price and nights.
- Unique booking reference.
- Transactional availability check and insert.

**Testing**
- One-night and multi-night total calculation.
- Invalid dates and invalid customer fields.
- Client-supplied price/total ignored or rejected by contract.
- Sold-out booking rejected.
- Two requests competing for the final room: only one succeeds when tested against MySQL/InnoDB.
- Reference collision retry and unique constraint.
- Transaction rollback when persistence fails.

**Completion criteria**
- Booking is persisted with `CONFIRMED`.
- Backend response contains the authoritative amount and reference.
- No overbooking in a real MySQL concurrency test.
- Document lock behavior and its throughput trade-off.

---

## Phase 5 — Booking retrieval and cancellation

**Objective:** Let a visitor retrieve and cancel their booking safely without accounts.

**Tasks**
- [ ] Define lookup request/response DTOs.
- [ ] Implement lookup by reference plus normalized email.
- [ ] Return generic not-found behavior for mismatch.
- [ ] Define cancellation request DTO containing email.
- [ ] Implement cancellation endpoint with reference and email verification.
- [ ] Require status `CONFIRMED`; reject repeated cancellation.
- [ ] Update status to `CANCELLED`; never delete the row.
- [ ] Ensure availability query excludes cancelled bookings.
- [ ] Return updated booking status and relevant booking summary.

**Files/modules to create/update**
- `BookingController.java`
- `BookingService.java`
- Lookup/cancel request DTOs and booking response DTO
- Repository methods for reference/email lookup and transactional status update
- Exception/error mapping
- Service and integration tests

**Dependencies:** Phase 4 booking records and status model.

**Deliverables**
- `POST /api/bookings/lookup`
- `PATCH /api/bookings/{reference}/cancel`
- Secure-by-contract reference + email verification.
- Availability released by status update.

**Testing**
- Correct reference + email returns booking.
- Wrong email and unknown reference return generic not-found response.
- Confirmed booking cancellation changes status.
- Cancelled booking cannot be cancelled again.
- Cancelled booking no longer occupies inventory.
- Cancellation transaction and concurrent state behavior.

**Completion criteria**
- Lookup and cancellation work against MySQL.
- No public all-bookings endpoint exists.
- Tests confirm cancelled bookings release availability.

---

## Phase 6 — React setup, Bootstrap, navigation, and home page

**Objective:** Establish the agreed visual system and first usable screen.

**Tasks**
- [ ] Configure React Router.
- [ ] Integrate Bootstrap 5.
- [ ] Establish sky-blue/white theme and typography.
- [ ] Create shared Navbar and Footer.
- [ ] Create reusable SearchBar.
- [ ] Implement Home page heading, subtitle, destination/date search, and Explore Stays cards.
- [ ] Use no guest-count selector; MVP books one room.
- [ ] Connect Explore Stays to real sample hotel data once API integration is available; avoid hardcoded fake availability.

**Files/modules to create**
- `App.jsx`, `index.js`
- `pages/Home.jsx`
- `components/Navbar.jsx`, `Footer.jsx`, `SearchBar.jsx`, `HotelCard.jsx` as needed
- `services/api.js` foundation
- `styles/theme.css`, `global.css`, `components.css`
- Hotel image assets

**Dependencies:** Phase 1 frontend setup; Phase 3 hotel API contract for live data.

**Deliverables**
- Responsive home page.
- Navigation and search form.
- Consistent brand styling.

**Testing**
- Form validation for destination and dates.
- Check-in not in past and check-out after check-in.
- Responsive desktop/tablet/mobile layout.
- Navbar links work.

**Completion criteria**
- Home page is visually consistent and usable.
- Search submits valid destination/date context to results route.

---

## Phase 7 — Search results, hotel details, and booking screens

**Objective:** Build the remaining core pre-booking screens.

**Tasks**
- [ ] Implement Search Results page and compact editable search bar.
- [ ] Build horizontal hotel cards.
- [ ] Implement functional price-range, amenities, and rating filters.
- [ ] Implement sorting: price low-to-high, price high-to-low, rating high-to-low.
- [ ] Implement no-results, loading, and API-error states.
- [ ] Implement Hotel Details page with image carousel, hotel information, amenities, policy, room details, capacity, availability, dates, and estimated total.
- [ ] Implement Booking Details page with name/email/phone form and two-column summary.
- [ ] Show price/night, nights, and estimated total before confirmation.
- [ ] Make clear that backend recalculates the final total.
- [ ] Prevent duplicate form submission and display field-level errors.

**Files/modules to create**
- `pages/SearchResults.jsx`
- `pages/HotelDetails.jsx`
- `pages/BookingDetails.jsx`
- `components/HotelImageCarousel.jsx`
- `components/FilterPanel.jsx`
- `components/BookingSummary.jsx`
- Loading/error components as needed
- API service functions for search/details/availability

**Dependencies:** Phase 6 navigation/theme and Phase 3 read API contracts.

**Deliverables**
- Functional search, hotel details, and booking form screens.
- Search filters and sorting operate on actual returned data.
- Booking request is prepared using only approved request fields.

**Testing**
- Search result API success/empty/error.
- Filters and all three sort modes.
- Date edits preserve correct context.
- Sold-out room cannot proceed to booking.
- Booking form validation and duplicate-submit protection.
- Displayed estimate uses returned room price and dates, not a client-authoritative total.

**Completion criteria**
- User can move from Home → Results → Details → Booking Details.
- Every visible control works.
- No fake booking success is shown.

---

## Phase 8 — Backend and frontend integration

**Objective:** Connect the full user journey to the implemented REST APIs.

**Tasks**
- [ ] Finalize API base URL and CORS for local development.
- [ ] Connect search and hotel detail pages to live backend data.
- [ ] Connect availability refresh when dates change.
- [ ] Submit booking to backend.
- [ ] Display server validation and conflict errors in the form.
- [ ] Populate booking summary and confirmation route from the backend response.
- [ ] Implement My Bookings lookup form using reference + email.
- [ ] Implement cancellation confirmation dialog and API call.
- [ ] Ensure cancelled status is reflected in UI.
- [ ] Handle network/server failure and loading states throughout.

**Files/modules to create/update**
- `services/api.js`
- `pages/Home.jsx`, `SearchResults.jsx`, `HotelDetails.jsx`, `BookingDetails.jsx`, `MyBookings.jsx`
- Shared error/loading components
- Backend CORS/error handling only if integration requires it

**Dependencies:** Backend Phases 3–5 and frontend Phases 6–7.

**Deliverables**
- End-to-end working booking journey using MySQL.
- Retrieval and cancellation flow.
- No mock data in core transactional flows.

**Testing**
- Full happy path from search to confirmed booking.
- Two users attempting the last room.
- Backend rejects stale availability.
- Booking lookup with matching and mismatching email.
- Cancellation updates status and allows inventory reuse.
- API unavailable / malformed response / server validation error states.

**Completion criteria**
- Core MVP works end-to-end against local backend and MySQL.
- Frontend never claims success before backend success.
- API contracts match documentation.

---

## Phase 9 — Destination Postcard Confirmation

**Objective:** Implement the agreed surprise feature without weakening core booking behavior.

**Tasks**
- [ ] Design centred vintage postcard with cream textured paper and destination illustration.
- [ ] Add Transit & Tile stamp/logo and booking reference styled like postal tracking code.
- [ ] Show dynamic greeting based on destination.
- [ ] Show hotel, guest name, check-in, and check-out from confirmed booking response.
- [ ] Show predefined sample weather with an explicit “illustrative sample” label; do not imply a live forecast.
- [ ] Show one curated attraction and one local tip; use generic safe fallback for unsupported destinations.
- [ ] Add flip animation.
- [ ] Implement Download Postcard action; if reliable download cannot be completed, remove or clearly disable the action rather than faking it.
- [ ] Implement View My Booking and Go Home navigation.

**Files/modules to create**
- `pages/BookingConfirmation.jsx`
- `components/` postcard component if reusable separation helps
- `styles/postcard.css`
- `assets/postcard/` assets

**Dependencies:** Phase 8 successful booking response and navigation.

**Deliverables**
- Distinctive, responsive confirmation postcard populated with real booking data.
- Functional actions.

**Testing**
- Postcard only appears after successful booking.
- Reference and booking details exactly match backend response.
- Unsupported destination uses generic content.
- Weather is labelled illustrative.
- Download and navigation actions work.
- Layout remains readable on mobile.

**Completion criteria**
- Surprise feature is complete without any fake forecast or fake booking state.
- Core booking remains functional if postcard animation/assets fail.

---

## Phase 10 — Automated testing, bug fixing, screenshots, and documentation

**Objective:** Verify the complete application, fix critical issues, and prepare a defensible submission.

**Tasks**
- [ ] Run all backend unit tests.
- [ ] Run MySQL integration tests for schema, overlap query, transactions, and concurrency.
- [ ] Run frontend build and manual UI test checklist.
- [ ] Test all date boundaries and cancellation edge cases.
- [ ] Test booking reference uniqueness and duplicate submissions.
- [ ] Review validation and error consistency.
- [ ] Check responsive layouts and image loading.
- [ ] Capture screenshots of all six screens and important states.
- [ ] Complete README prerequisites, environment variables, DB setup, sample data, run/test instructions.
- [ ] Complete DESIGN.md: architecture, relationships, constraints, APIs, booking logic, overlap rule, locking, assumptions, trade-offs, surprise feature, limitations, improvements.
- [ ] Confirm no secrets, local DB dumps, or personal data are committed.
- [ ] Verify public GitHub repository contents and assignment submission instructions.
- [ ] Record approximate time spent and any unfinished items honestly.

**Files/modules to create/update**
- Backend and frontend tests.
- `README.md`
- `DESIGN.md`
- `docs/` screenshots/demo assets or links
- `.gitignore`
- SQL setup/seed scripts or approved migration artifacts
- Final API examples and test evidence

**Dependencies:** All previous phases.

**Deliverables**
- Verified runnable MVP.
- Tests and actual test results.
- Documentation and screenshots/demo.
- Submission-ready repository.

**Testing**
- Run full backend test suite.
- Run frontend production build.
- Execute manual end-to-end checklist.
- Repeat MySQL concurrency test.
- Test clean setup using README instructions if time permits.

**Completion criteria**
- Required user journeys work.
- No critical overbooking, pricing, retrieval, or cancellation defects.
- Documentation reflects actual implementation, not planned features.
- Unfinished items are explicitly disclosed.

---

## Cross-phase acceptance checklist

### Booking correctness
- [ ] Check-in cannot be in the past.
- [ ] Check-out is after check-in.
- [ ] Nights are computed as date difference.
- [ ] Overlap predicate uses exclusive check-out boundary.
- [ ] Only confirmed bookings consume inventory.
- [ ] Cancelled bookings release inventory.
- [ ] Backend recalculates availability inside transaction.
- [ ] Competing requests cannot both consume the final room.

### Financial correctness
- [ ] Price comes from MySQL.
- [ ] Total is calculated on backend using `BigDecimal`.
- [ ] Client-supplied price/total is not trusted.
- [ ] Booking response includes the actual calculated breakdown.

### API and privacy
- [ ] DTOs are used; entities are not exposed.
- [ ] Lookup requires reference and email.
- [ ] Cancellation requires reference and email.
- [ ] Mismatch does not disclose booking data.
- [ ] No all-customer bookings endpoint exists.
- [ ] Error responses are consistent and do not leak internals.

### UX and submission
- [ ] Six agreed screens exist.
- [ ] Filters/sorting work.
- [ ] Loading, empty, and error states exist.
- [ ] Postcard uses confirmed backend data.
- [ ] Sample weather is clearly illustrative.
- [ ] Screenshots and setup/design documentation are included.

## Open decisions to resolve before the relevant phase

1. **Historical price snapshot:** approve or reject `price_per_night_at_booking` in `BOOKING`. Recommendation: approve.
2. **Amenities and images representation:** choose MySQL JSON columns or bounded text consistently. Recommendation: JSON if the selected MySQL version supports it reliably; otherwise text with controlled serialization.
3. **Date context:** use India/local assignment context for “today” validation and document it; ensure backend and browser agree.
4. **MySQL test strategy:** use MySQL/InnoDB integration tests for concurrency; mocked unit tests are insufficient.
5. **Frontend project generator:** select the simplest maintained React setup at Phase 1; do not introduce a different UI framework.

These are implementation details to settle at the relevant phase; they do not block creating the documentation workspace. No decision above authorizes changing the agreed stack or adding scope.
