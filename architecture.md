# TRANSIT & TILE — Technical Architecture Blueprint

**Status:** Foundation blueprint; no application code has been created yet.  
**Scope:** Functional hotel-booking MVP plus Destination Postcard Confirmation.  
**Stack:** Java 17, Spring Boot, Spring Data JPA/Hibernate, Maven, MySQL, React, Bootstrap 5, custom CSS, React Router, JUnit 5, Mockito.

## 1. System architecture

Transit & Tile is a browser-based single-page React application backed by a Spring Boot REST API and a MySQL relational database.

```text
Visitor
  |
  v
React SPA (React Router, Bootstrap 5, custom CSS)
  |
  | HTTP / JSON
  v
Spring Boot REST API
  |
  +--> Controllers (HTTP boundary and DTO validation)
  |
  +--> Services (search, availability, booking, cancellation, reference generation)
  |
  +--> Repositories (Spring Data JPA queries and locking)
  |
  +--> JPA Entities
  |
  v
MySQL (HOTEL, ROOM_TYPE, BOOKING)
```

The backend is authoritative for dates, availability, booking references, price calculation, booking status, retrieval verification, and cancellation. The frontend presents data, collects input, performs user-friendly validation, and renders responses. It must never be trusted to determine whether a room is available or what a booking costs.

No authentication, payment gateway, admin dashboard, live forecast provider, or deployment platform is part of this design.

## 2. Technology and responsibility matrix

| Area | Technology | Responsibility |
|---|---|---|
| Backend language | Java 17 | Domain and API implementation |
| Backend framework | Spring Boot | Application configuration and REST hosting |
| Persistence | Spring Data JPA / Hibernate | Entity mapping and database access |
| Build | Maven | Dependency and build lifecycle |
| Database | MySQL / InnoDB | Durable hotel, room, and booking data |
| Frontend | React | Interactive SPA |
| Navigation | React Router | Screen routing |
| UI | Bootstrap 5 + custom CSS | Responsive layout and brand styling |
| API | REST / JSON | Frontend-backend contract |
| Backend tests | JUnit 5 + Mockito | Unit and service behavior tests |

Select a Spring Boot release compatible with Java 17 when project setup begins and record the exact version. Avoid adding libraries unless a requirement cannot reasonably be met with the agreed stack.

## 3. Proposed workspace structure

The following is the intended directory structure. Directories may be created during workspace initialization; application files are added only in their approved implementation phases.

```text
TRANSIT_AND_TILE/
├── SKILL.md
├── architecture.md
├── implementation_plan.md
├── README.md                         # create during setup/documentation phase
├── DESIGN.md                         # create/update during documentation phase
├── .gitignore                        # create during setup phase
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/transitandtile/
│       │   │   ├── TransitAndTileApplication.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── dto/
│       │   │   │   ├── request/
│       │   │   │   └── response/
│       │   │   └── exception/
│       │   └── resources/
│       │       └── application.properties
│       └── test/java/com/transitandtile/
│           ├── service/
│           ├── repository/
│           └── controller/
├── frontend/
│   ├── package.json
│   ├── public/
│   └── src/
│       ├── assets/
│       │   ├── hotels/
│       │   └── postcard/
│       ├── components/
│       ├── pages/
│       ├── services/
│       ├── styles/
│       ├── App.jsx
│       └── index.js
└── docs/
```

The tree shows planned application files for clarity. They are not to be created as empty source placeholders during foundation setup. `docs/` is available for supporting project material. Keep the three domain tables in the same MySQL schema.

### Backend package responsibilities

| Package | Responsibility | Likely modules (created in later phases) |
|---|---|---|
| `controller` | HTTP endpoints, request DTO binding, response status | `HotelController`, `BookingController` |
| `service` | Business rules and transaction boundaries | `HotelService`, `AvailabilityService`, `BookingService`, `BookingReferenceGenerator` |
| `repository` | Persistence queries, overlap query, lock query | `HotelRepository`, `RoomTypeRepository`, `BookingRepository` |
| `entity` | JPA-mapped database records | `Hotel`, `RoomType`, `Booking`, `BookingStatus` |
| `dto/request` | Validated inbound API contracts | Search, create booking, lookup, cancel requests |
| `dto/response` | Outbound API contracts | Hotel cards/details, availability, booking, error response |
| `exception` | Domain exceptions and centralized API error mapping | Not-found, sold-out/conflict, global exception handler |
| `config` | Narrow application configuration | CORS and other required configuration only |

Use constructor injection. Controllers must not contain database queries or booking calculations. Services must not return JPA entities to controllers.

### Frontend organization

| Location | Responsibility |
|---|---|
| `pages/` | Route-level screens: Home, Search Results, Hotel Details, Booking Details, Booking Confirmation, My Bookings |
| `components/` | Shared Navbar, Footer, SearchBar, HotelCard, image carousel, filters, booking summary, loading/error states |
| `services/` | Central API functions and API base URL configuration |
| `styles/` | Global theme, component styles, postcard styles |
| `assets/hotels/` | Local hotel imagery used by sample data/UI |
| `assets/postcard/` | Postcard textures, stamps, and decorative assets |

Use React Router routes:
- `/`
- `/search`
- `/hotels/:hotelId`
- `/booking`
- `/confirmation/:reference`
- `/my-bookings`

Do not place customer email or phone in route parameters or query strings. Carry only non-sensitive selection context through navigation; reload authoritative booking data from the backend where needed.

## 4. Database design

The core design uses exactly three domain tables: `HOTEL`, `ROOM_TYPE`, and `BOOKING`. MySQL should use InnoDB.

### 4.1 HOTEL

| Column | Suggested MySQL type | Rules / purpose |
|---|---|---|
| `hotel_id` | `BIGINT` | Primary key, generated |
| `name` | `VARCHAR(150)` | Required |
| `location` | `VARCHAR(150)` | Required; searchable destination/city text |
| `description` | `TEXT` | Required or non-empty sample description |
| `amenities` | `JSON` or bounded text | List of amenity labels; choose one representation consistently |
| `rating` | `DECIMAL(2,1)` | Demo rating; constrain to agreed range, e.g. 0–5 |
| `image_urls` | `JSON` or bounded text | Image paths/URLs for carousel; no fourth table |
| `check_in_time` | `TIME` | Display policy |
| `check_out_time` | `TIME` | Display policy |
| `cancellation_policy` | `VARCHAR(500)` or `TEXT` | Display policy |

**Indexes:** primary key; index on normalized/searchable `location` as supported by the chosen query. Search should be case-insensitive according to a deliberate MySQL collation/normalization choice.

### 4.2 ROOM_TYPE

| Column | Suggested MySQL type | Rules / purpose |
|---|---|---|
| `room_type_id` | `BIGINT` | Primary key, generated |
| `hotel_id` | `BIGINT` | Required FK to `HOTEL`; unique to enforce one category per hotel |
| `room_name` | `VARCHAR(100)` | e.g. Deluxe Room |
| `description` | `TEXT` | Room description |
| `price_per_night` | `DECIMAL(10,2)` | Required and greater than zero |
| `capacity` | `INT` | Positive maximum occupancy per room; informational in MVP because guest count is fixed to one |
| `total_rooms` | `INT` | Positive inventory count |

**Constraints/indexes:** FK to `HOTEL`; unique index on `hotel_id`; checks for positive price, capacity, and room count. `hotel_id` unique means each hotel has one room category, not one physical room.

### 4.3 BOOKING

| Column | Suggested MySQL type | Rules / purpose |
|---|---|---|
| `booking_id` | `BIGINT` | Primary key, generated |
| `room_type_id` | `BIGINT` | Required FK to `ROOM_TYPE` |
| `booking_reference` | `VARCHAR(30)` | Required unique public reference |
| `customer_name` | `VARCHAR(120)` | Required |
| `email` | `VARCHAR(254)` | Required; normalized before storage |
| `phone` | `VARCHAR(25)` | Required under MVP form |
| `check_in` | `DATE` | Required |
| `check_out` | `DATE` | Required; later than check-in |
| `total_amount` | `DECIMAL(10,2)` | Backend-calculated total |
| `status` | `VARCHAR(20)` | `CONFIRMED` or `CANCELLED` |
| `created_at` | `TIMESTAMP` | Required creation audit time |

**Recommended audit improvement requiring owner approval before implementation:** add `price_per_night_at_booking DECIMAL(10,2)` to this same table. Without it, a later change to the room's current price makes it difficult to explain the historical amount. The stored `total_amount` remains the amount actually charged/quoted for the booking. This recommendation does not add a fourth table.

**Constraints/indexes:**
- FK `room_type_id` → `ROOM_TYPE.room_type_id`.
- Unique index on `booking_reference`.
- Check `check_out > check_in`.
- Check `total_amount >= 0`.
- Index supporting availability lookup, starting with `room_type_id` and `status`, with date columns selected after query-plan verification.
- Consider index on `email` only if useful for lookup; the lookup must always use both reference and email.
- Do not cascade-delete bookings when a hotel or room type is removed. Historical records must be retained.

### 4.4 Entity relationships

```text
HOTEL 1 ───── 1 ROOM_TYPE 1 ───── many BOOKING
```

- A hotel has one room category for this assignment.
- A room type belongs to one hotel.
- A room type can have many bookings across different dates.
- A booking belongs to one room type.
- A booking is not deleted on cancellation; only its status changes.

`capacity` means the maximum guests allowed in one room. `total_rooms` means the number of physical rooms available in that category. These are different concepts. The current flow books one room and does not ask the user to select a guest count; capacity is displayed as room information, not used as inventory.

## 5. Core business rules

### 5.1 Stay dates and nights
- Check-in cannot be earlier than the current date.
- Check-out must be strictly later than check-in.
- Both are local calendar dates (`YYYY-MM-DD`).
- `nights = DAYS.between(checkIn, checkOut)`.
- A one-night booking has check-in on day 1 and check-out on day 2.

### 5.2 Overlapping stays
An existing booking overlaps the requested stay when:

```text
existing.check_in < requested.check_out
AND existing.check_out > requested.check_in
```

This is a half-open interval rule: check-in is inclusive and check-out is exclusive. A guest checking out on 10 June does not block another guest checking in on 10 June.

Only `CONFIRMED` bookings count as occupied. `CANCELLED` bookings are excluded.

### 5.3 Availability calculation
For the requested room type and date range:

```text
occupiedRooms = count of overlapping CONFIRMED bookings
availableRooms = totalRooms - occupiedRooms
```

The MVP books exactly one room, so the booking is accepted only when `availableRooms >= 1`. Search and details APIs may show `Available` / `Sold Out`, but this is an informational snapshot; the booking endpoint must always recheck availability transactionally.

### 5.4 Price calculation
The frontend sends room type ID, dates, and customer details. It does not send an authoritative price or total.

```text
nights = DAYS.between(checkIn, checkOut)
totalAmount = databaseRoomPricePerNight × nights
```

Use Java `BigDecimal` and MySQL `DECIMAL`, with a consistent scale/currency policy. Return the server-calculated breakdown in the successful booking response. Do not trust browser-calculated values.

## 6. REST API design

All endpoints use `/api`, JSON, and ISO date strings. Exact DTO field names should be finalized when implementing each phase and then kept consistent in backend, frontend, and documentation.

| Method | Endpoint | Purpose | Main success response |
|---|---|---|---|
| `GET` | `/api/hotels?location=...&checkIn=YYYY-MM-DD&checkOut=YYYY-MM-DD` | Search hotels and show date-specific availability | Hotel result DTO list |
| `GET` | `/api/hotels/{hotelId}` | Hotel details, room details, amenities, policies | Hotel detail DTO |
| `GET` | `/api/hotels/{hotelId}/availability?checkIn=...&checkOut=...` | Recalculate room availability for selected dates | Availability DTO |
| `POST` | `/api/bookings` | Create one-room booking | `201 Created`, booking DTO |
| `POST` | `/api/bookings/lookup` | Retrieve booking using reference and email in request body | Booking DTO |
| `PATCH` | `/api/bookings/{reference}/cancel` | Cancel after verifying email in request body | Updated booking DTO |

### 6.1 Search request and response expectations

Search parameters:
- `location` (required, trimmed)
- `checkIn` (required ISO date)
- `checkOut` (required ISO date)

Validate dates and location. Search returns hotels matching the destination and a date-specific availability indication. If no results, return an empty list with HTTP 200; the frontend displays a useful no-results state. Do not include a result count if the agreed UI does not call for one.

### 6.2 Booking creation contract

Request should contain only:
- `roomTypeId`
- `checkIn`
- `checkOut`
- `customerName`
- `email`
- `phone`

It must not accept `totalAmount`, `pricePerNight`, `bookingReference`, or `status` as authoritative values.

Success response should include:
- booking reference
- booking status
- hotel name/location
- room name
- customer name
- check-in/check-out
- number of nights
- price per night used
- total amount
- creation time where useful

The response is the source for the confirmation postcard and booking summary.

### 6.3 Lookup and cancellation contracts

`POST /api/bookings/lookup` body: booking reference + email. Return the booking only if both match. Use a generic not-found response on mismatch to avoid disclosing whether a reference belongs to another email.

`PATCH /api/bookings/{reference}/cancel` body: email. Verify the email, require current status `CONFIRMED`, and update status to `CANCELLED`. A repeated cancellation should return `409 Conflict` with a clear state message. Never put email in the URL.

### 6.4 Error response contract

Use a consistent shape, for example:

```json
{
  "timestamp": "2026-11-12T10:00:00",
  "status": 409,
  "code": "ROOM_UNAVAILABLE",
  "message": "No rooms are available for the selected dates.",
  "fieldErrors": []
}
```

This is an illustrative contract, not an implemented response. Use:
- `400 Bad Request`: invalid fields or date range.
- `404 Not Found`: hotel not found or booking lookup not found/mismatched.
- `409 Conflict`: no inventory remains, duplicate reference after bounded retries, or invalid booking state transition.
- `500 Internal Server Error`: unexpected server failure.

Never return SQL messages, stack traces, or internal exception details.

## 7. Workflows

### 7.1 Hotel search
1. Visitor enters destination, check-in, and check-out.
2. React performs immediate usability validation.
3. Search page calls `GET /api/hotels` with ISO dates.
4. Backend validates input and finds matching hotels.
5. Backend calculates availability for each hotel’s room type and date range.
6. React displays cards, price per night, rating, amenities, and availability.
7. Filters and sorting operate on the returned result set; they must be functional, not decorative.
8. Empty result state suggests changing destination or dates.

### 7.2 Hotel details
1. Visitor opens `/hotels/{hotelId}`.
2. React requests hotel details.
3. Backend returns hotel, room category, policy, and image data.
4. Availability is checked for the currently selected dates.
5. UI displays `Available` or `Sold Out`, price/night, number of nights, and estimated total.
6. The estimate is clearly provisional; backend recalculates during booking.
7. User may edit dates and return to search.

### 7.3 Booking creation
1. User enters name, email, and phone; one room is implied.
2. Frontend validates required fields and displays a summary.
3. Frontend sends room type ID, dates, and customer details only.
4. Backend validates request and starts a transaction.
5. Backend locks the room type row with a pessimistic write lock.
6. Backend rechecks confirmed overlapping bookings.
7. If no inventory remains, transaction ends with `409 Conflict`.
8. Backend calculates nights and amount from the database room price.
9. Backend generates a unique reference and persists the booking as `CONFIRMED`.
10. Transaction commits.
11. Only after receiving successful response does React show the confirmation postcard.

### 7.4 Booking retrieval
1. User enters booking reference and email.
2. React posts both values in the request body.
3. Backend normalizes and matches both values.
4. If matched, return a DTO with booking and status details.
5. If not matched, return generic `404` without revealing booking existence.

### 7.5 Cancellation
1. User selects Cancel Booking.
2. UI displays a confirmation dialog before the request.
3. User confirms; React sends email in the request body.
4. Backend verifies reference and email, and checks status.
5. If confirmed, update status to `CANCELLED` in a transaction.
6. The booking record remains for history.
7. Availability calculation excludes it, releasing the room for those dates.
8. UI displays the updated `CANCELLED` status. Repeated cancellation is rejected.

## 8. Transaction and concurrency design

### Risk
A non-transactional sequence of “count bookings, see one room free, insert” can overbook. Two simultaneous requests can both read the same availability before either inserts.

### Agreed practical protection
Use a service-level transaction and a pessimistic write lock on the `ROOM_TYPE` row. Every booking request for the same room type must lock that row before checking availability. Under InnoDB, this serializes competing booking transactions for that room type.

Within one transaction:
1. Acquire the room type lock.
2. Load inventory and current price.
3. Count overlapping `CONFIRMED` bookings using the half-open overlap predicate.
4. Reject if no room remains.
5. Calculate nights and total.
6. Generate reference.
7. Insert booking.
8. Commit.

Keep the transaction short and do not call external services while holding the lock. Use a consistent lock order for booking and cancellation paths. Handle unique-reference collisions with a bounded retry. Consider deadlock/lock-timeout handling and return a controlled conflict/retryable error.

**Important limitation:** a unit test with mocked repositories cannot prove MySQL locking. Validate the lock query and concurrent booking behavior against MySQL/InnoDB in an integration test. A pessimistic lock is appropriate for this small assignment but serializes requests for the same room type, which trades throughput for straightforward correctness.

## 9. Frontend-backend communication

- Centralize API base URL and request helpers in `frontend/src/services`.
- Use one HTTP strategy consistently (browser Fetch API is sufficient for this project).
- Send and receive JSON with correct content types.
- Handle loading, empty, success, validation, and server-error states.
- Keep request/response DTO names synchronized with the API contract.
- Never show booking confirmation based on frontend-only state.
- Use the booking response as the source for confirmation and booking details.
- Do not store sensitive personal information in URLs or browser console logs.
- For refresh behavior, the confirmation route may retrieve the booking through the reference and a user-supplied email flow; do not bypass email verification to fetch private booking data.

## 10. Security and privacy considerations

This assignment has no login. Therefore, reference plus email is the agreed lightweight retrieval/cancellation verification, not robust authentication.

Practical safeguards:
- Verify both reference and normalized email for lookup and cancellation.
- Use generic not-found behavior on mismatch.
- Never expose a public “list all bookings” endpoint.
- Do not log full personal payloads.
- Use parameterized JPA/repository queries.
- Configure CORS only for the local frontend origin during development.
- Store database credentials in environment variables; commit placeholders only.
- Do not expose stack traces, SQL, or secrets in API errors.
- Apply input length/format validation.
- Keep sample customer data fictional.

## 11. Visual and UX architecture

Brand direction: sky-blue and white, clean modern sans-serif, restrained layout, responsive cards and forms. The home page uses a centred search area and an Explore Stays section. Results use horizontal hotel cards and functional filters/sorting. Hotel details show image carousel, policy, room information, and selected dates. Booking details use a two-column form/summary layout.

The postcard is a centred vintage cream-paper composition with destination illustration, stamp/logo, unique booking reference, dynamic greeting, illustrative sample weather, one curated attraction and a generic or curated local tip, plus hotel/date/guest details. Use a flip animation only after confirmed backend success. Download must work when implemented; do not leave a fake button.

## 12. Technical concerns and recommendations

| Concern | Risk | Recommendation / status |
|---|---|---|
| Historical room price | If current room price changes, old booking economics become unclear | Recommend `price_per_night_at_booking` in `BOOKING`; owner approval required before implementation |
| Availability query/index | Overlap predicates use inequalities on both date columns; one generic composite index may not optimize all cases | Add a practical index, inspect MySQL `EXPLAIN`, and document measured choice |
| Concurrent booking | Check-then-insert without serialization overbooks | Pessimistic write lock on `ROOM_TYPE` inside transaction; verify with MySQL integration test |
| Reference collision | Random readable references can collide | Unique DB constraint and bounded retry |
| No authentication | Reference/email can be guessed or exposed; email is not strong identity proof | Require both values, generic mismatch response, no list endpoint; document residual risk |
| Public demo ratings | Invented ratings could mislead | Label ratings as sample/demo values |
| Weather | Predefined sample weather is not a forecast | Label explicitly as illustrative sample weather |
| Images and schema | Carousel needs multiple images but only three tables are allowed | Store image paths/URLs as JSON or bounded text; do not add image table |
| Room capacity | Capacity is not the same as room inventory | Keep `capacity` and `total_rooms` separate; no guest selector in current MVP |
| Time zone/date | “Today” depends on local date context | Agree the application date context (India/local assignment context) and use it consistently for check-in validation |

## 13. Definition of architectural completion

The architecture is ready for implementation when:
- Three-table schema and constraints are agreed.
- Request/response DTO fields are fixed per endpoint.
- Date overlap and availability behavior are tested.
- Booking uses backend price and transaction/lock protection.
- Lookup/cancel both verify reference and email.
- All six screens map to documented routes.
- Postcard uses backend-confirmed data and labels weather as illustrative.
- Scope exclusions remain explicit.
