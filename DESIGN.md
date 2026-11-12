\# Transit \& Tile – Design Document



\## 1. Project Overview



Transit \& Tile is a full-stack hotel booking portal that allows users to search hotels, check room availability for selected dates, make reservations, retrieve booking details, and cancel existing bookings.



The application follows a client-server architecture. The React frontend communicates with a Spring Boot REST API, while MySQL stores hotel, room category, and booking information.



The project focuses on reliable booking operations, accurate server-side pricing, inventory management, and a simple user experience.



\## 2. Technology Stack



| Layer | Technology |

|---|---|

| Frontend | React 19, React Router 7 |

| Styling | Bootstrap 5, Custom CSS |

| Backend | Java 17, Spring Boot 3.3.6 |

| API | Spring MVC REST APIs |

| Persistence | Spring Data JPA, Hibernate |

| Database | MySQL 8 |

| Testing | JUnit 5, Mockito, Spring Boot Test |

| Build tools | Maven, Vite, npm |



\## 3. System Architecture



The application is divided into three main layers:



\### Frontend

\- Built using React and Vite.

\- Provides hotel search, hotel details, booking forms, booking confirmation, and booking lookup/cancellation.

\- Uses React Router for navigation.

\- Communicates with backend endpoints through HTTP requests.



\### Backend

\- Built using Spring Boot.

\- Controllers expose REST endpoints.

\- Service classes contain business logic such as availability checks, booking creation, pricing, and cancellation.

\- Repositories use Spring Data JPA to access MySQL.

\- DTOs separate API request/response structures from database entities.

\- A centralized exception handler provides consistent API error responses.



\### Database

\- MySQL stores hotel information, room categories, and reservations.

\- Foreign keys maintain relationships between records.

\- Unique constraints and indexes support data integrity and query performance.



\## 4. Database Design



The database contains three main tables.



\### HOTEL

Stores hotel information:

\- hotel\_id – Primary key

\- name

\- location

\- description

\- amenities

\- rating

\- image\_urls

\- check\_in\_time

\- check\_out\_time

\- cancellation\_policy



An index on location supports hotel searches.



\### ROOM\_TYPE

Stores room category and inventory information:

\- room\_type\_id – Primary key

\- hotel\_id – Foreign key referencing HOTEL

\- room\_name

\- description

\- price\_per\_night

\- capacity

\- total\_rooms



The hotel\_id column has a unique constraint. Therefore, the current implementation supports exactly one room category per hotel.



\### BOOKING

Stores customer reservation information:

\- booking\_id – Primary key

\- room\_type\_id – Foreign key referencing ROOM\_TYPE

\- booking\_reference – Unique booking reference

\- customer\_name

\- email

\- phone

\- check\_in

\- check\_out

\- price\_per\_night\_at\_booking

\- total\_amount

\- status

\- created\_at



The price\_per\_night\_at\_booking field preserves the original price at the time of reservation.



\### Relationships



\- HOTEL to ROOM\_TYPE: One-to-one.

\- ROOM\_TYPE to BOOKING: One-to-many.

\- Each booking belongs to one room category.

\- A room category can have multiple bookings across different dates.



Amenities and image URLs are represented as lists in the Java entity and persisted using a JPA attribute converter.



\## 5. API Design



| Method | Endpoint | Purpose |

|---|---|---|

| GET | /api/hotels | Search hotels by optional location |

| GET | /api/hotels/{hotelId} | Retrieve hotel details |

| GET | /api/hotels/{hotelId}/availability | Check room availability and estimated price |

| POST | /api/bookings | Create a reservation |

| POST | /api/bookings/lookup | Retrieve a reservation using reference and email |

| PATCH | /api/bookings/{reference}/cancel | Cancel a reservation |



The booking creation endpoint returns HTTP 201 when a reservation is successfully created.



The controllers use request and response DTOs instead of directly exposing JPA entities.



\## 6. Booking and Availability Logic



\### Date Validation



The application validates that:

\- Both check-in and check-out dates are provided.

\- Check-in cannot be in the past.

\- Check-out must be strictly after check-in.



Date validation uses the Asia/Kolkata timezone.



\### Availability Calculation



The application counts overlapping CONFIRMED bookings for the requested room category.



Two stays overlap when:



`existing.checkIn < requested.checkOut`



AND



`existing.checkOut > requested.checkIn`



This uses a half-open date interval. Therefore, a guest can check in on the same day another guest checks out.



Cancelled bookings are excluded from availability calculations.



Available rooms are calculated as:



`Available Rooms = Total Rooms - Overlapping Confirmed Bookings`



The result is never allowed to be negative.



\### Pricing



The backend calculates the booking amount using:



`Total Amount = Price Per Night × Number of Nights`



The room price is retrieved from the database. The backend does not accept or trust a price supplied by the frontend.



BigDecimal is used for monetary calculations to avoid floating-point precision issues.



\## 7. Concurrency and Double-Booking Prevention



A key challenge in a hotel booking system is preventing two customers from booking the last available room simultaneously.



The application addresses this using pessimistic write locking.



During booking creation:



1\. The backend validates the requested dates.

2\. It obtains a pessimistic write lock on the relevant ROOM\_TYPE database row using Spring Data JPA's PESSIMISTIC\_WRITE lock mode.

3\. It checks overlapping confirmed bookings while holding the lock.

4\. If inventory is available, it calculates the final price.

5\. It generates a unique booking reference and saves the booking.

6\. The transaction commits and releases the lock.



Competing booking requests for the same room category must wait for the lock to be released. They then perform their own availability check before creating a reservation.



The booking operation is transactional, keeping the inventory check and booking insertion within the same transaction boundary.



The concurrency test suite includes a test against MySQL/InnoDB to verify the locking behaviour with real database transactions.



\## 8. Booking Reference and Cancellation



\### Booking Reference



Each reservation receives a generated unique booking reference.



A database uniqueness constraint protects against duplicate references. If a collision occurs, the application retries generation up to five times.



\### Booking Lookup



A reservation is retrieved only when both the booking reference and customer email match.



Email comparison is case-insensitive, and the input is normalized before lookup.



\### Cancellation



Cancellation changes the booking status from CONFIRMED to CANCELLED.



The booking record is not deleted. This preserves reservation history while allowing cancelled inventory to become available again.



Attempting to cancel an already-cancelled booking returns a conflict response.



\## 9. Validation and Error Handling



Request DTOs use Jakarta Bean Validation annotations such as:

\- @NotNull

\- @NotBlank

\- @Email

\- @Size

\- @Positive

\- @Pattern



A centralized GlobalExceptionHandler handles application and request errors.



Examples include:



| Situation | HTTP Status |

|---|---|

| Invalid request or date range | 400 Bad Request |

| Validation failure | 400 Bad Request |

| Hotel or booking not found | 404 Not Found |

| Room unavailable | 409 Conflict |

| Booking already cancelled | 409 Conflict |

| Unexpected server error | 500 Internal Server Error |



Error responses contain a status code, error identifier, and message. Internal SQL details and stack traces are not exposed to clients.



\## 10. Additional Feature – Travel-Themed Booking Confirmation



The booking confirmation page presents reservation details in a travel-postcard format.



It includes destination-specific greetings and travel information, along with the booking reference, guest details, stay dates, room information, and total price.



Users can download the confirmation postcard as a PNG image.



Weather information shown on the postcard is illustrative and is not retrieved from a live weather service.



\## 11. Assumptions and Trade-offs



\- The application uses fictional sample hotel data for demonstration.

\- Each hotel has one room category in the current database design.

\- Each booking represents one room.

\- User authentication and online payment processing are outside the current scope.

\- Booking lookup and cancellation require the booking reference and customer email.

\- Availability is checked again during booking creation to handle concurrent requests.

\- Weather and destination information on the postcard are illustrative.

\- The application is intended as an assessment prototype rather than a production booking platform.



\## 12. Challenges and Solutions



\### Preventing Double Booking

A simple availability check before insertion could allow concurrent requests to reserve the same remaining inventory. Pessimistic locking and a transactional recheck were implemented to address this.



\### Maintaining Accurate Prices

Frontend-calculated prices could be manipulated or become outdated. The backend retrieves the current room price and calculates the total independently.



\### Handling Booking History

Deleting cancelled reservations would remove useful historical information. Instead, the application updates the booking status to CANCELLED.



\### Consistent Error Responses

Different exceptions could otherwise produce inconsistent responses. A centralized exception handler provides structured error responses.



\## 13. Future Improvements



Potential enhancements include:

\- User registration, authentication, and role-based access.

\- Multiple room categories per hotel.

\- Online payment gateway integration.

\- Admin dashboard for managing hotels, room inventory, and reservations.

\- Live weather API integration.

\- Email booking confirmations and cancellation notifications.

\- Deployment to a cloud hosting platform.

\- Enhanced monitoring, audit logging, and production security controls.



\## 14. Conclusion



Transit \& Tile demonstrates a full-stack hotel booking workflow using React, Spring Boot, and MySQL. The implementation focuses on backend-controlled pricing, date-based inventory management, transactional booking operations, concurrency handling, structured API validation, and an accessible booking experience.

