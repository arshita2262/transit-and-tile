# Transit & Tile - Hotel Booking Portal

A full-stack hotel booking application built with Java, Spring Boot, MySQL, React, and Vite. Users can search hotels, check room availability, make reservations, view booking details, and cancel bookings.

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.3.6 |
| Backend architecture | Spring MVC, Spring Data JPA, Hibernate |
| Database | MySQL 8 |
| Frontend | React 19, React Router 7 |
| Styling | Bootstrap 5, custom CSS |
| Build tools | Maven, Vite, npm |
| Testing | JUnit 5, Mockito, Spring Boot Test |

## Features

- Search hotels by destination and travel dates.
- View hotel details, amenities, room information, and pricing.
- Check room availability for selected dates.
- Create bookings with customer details.
- Generate a unique booking reference for each reservation.
- Calculate booking prices on the backend.
- Look up reservations using booking reference and email.
- Cancel existing reservations.
- View booking confirmation as a travel-themed postcard.
- Download the booking confirmation postcard as a PNG image.
- View and filter bookings in the My Bookings section.

## Application Screens

1. Home
2. Search Results
3. Hotel Details
4. Booking Details
5. Booking Confirmation
6. My Bookings

## Prerequisites

Install the following before running the application:

| Software | Required version |
|---|---|
| Java JDK | 17 or later |
| Apache Maven | 3.9 or later |
| MySQL Community Server | 8.0 or later |
| Node.js | 18 or later |
| npm | 9 or later |

TRANSIT_AND_TILE_workspace/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/transitandtile/
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── schema.sql
│       │       └── data.sql
│       └── test/java/com/transitandtile/
├── frontend/
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── pages/
│       ├── components/
│       ├── services/
│       ├── utils/
│       └── styles/
├── .env.example
├── .gitignore
├── README.md
└── DESIGN.md

## Database Setup

1. Start your MySQL server.
2. Create the application database:

```sql
CREATE DATABASE IF NOT EXISTS transit_and_tile_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

3. The backend automatically executes the following SQL scripts at startup:

- `backend/src/main/resources/schema.sql` - Creates the HOTEL, ROOM_TYPE, and BOOKING tables.
- `backend/src/main/resources/data.sql` - Inserts five sample hotels and their corresponding room categories.

The database contains one room category per hotel in the current implementation.

## Environment Variables

The backend reads database connection details from environment variables. Do not commit real database credentials.

Required variables:

| Variable | Description | Default |
|---|---|---|
| `DB_HOST` | MySQL host | localhost |
| `DB_PORT` | MySQL port | 3306 |
| `DB_NAME` | Database name | transit_and_tile_db |
| `DB_USERNAME` | MySQL username | Required |
| `DB_PASSWORD` | MySQL password | Required |

### Windows PowerShell setup

Open PowerShell and set the variables:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "transit_and_tile_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
```

Replace the username and password with your local MySQL credentials. These values are for your local terminal session and should not be committed to Git.

## Running the Backend

From the project root:

```powershell
cd backend
mvn spring-boot:run
```

The Spring Boot application starts at:

`http://localhost:8081`

Keep the backend terminal running.

## Running the Frontend

Open a second terminal from the project root:

```powershell
cd frontend
npm install
npm run dev
```

The frontend runs at:

`http://localhost:5173`

The Vite development server proxies API requests to the backend at port 8081.

## Running Tests

### Backend tests

From the backend directory:

```powershell
mvn clean test
```

The test suite covers application context loading, controllers, services, repositories, booking integration, availability, and concurrency behaviour.

**Note:** MySQL integration and concurrency tests require a running MySQL instance and the configured database credentials. H2 is used for the application context smoke test; it does not replace MySQL/InnoDB concurrency testing.

### Frontend linting

From the frontend directory:

```powershell
npm run lint
```

### Frontend production build

```powershell
npm run build
```

## Design and Implementation

- The backend exposes REST APIs for hotel search, availability, booking, reservation lookup, and cancellation.
- Spring Data JPA and Hibernate handle database persistence.
- Booking prices are calculated by the backend using the selected room's price and stay duration.
- Availability checks consider overlapping booking dates.
- Pessimistic database locking is used in the booking flow to protect room inventory during concurrent booking attempts.
- The frontend communicates with the backend through HTTP requests.
- The booking confirmation postcard provides a downloadable visual summary of the reservation.

For further details about the architecture, database design, API design, booking logic, assumptions, and limitations, see `DESIGN.md`.

## Assumptions and Limitations

- The application uses fictional sample hotel data for demonstration.
- Each hotel currently has one room category.
- User authentication and online payment processing are not implemented.
- Booking lookup uses a booking reference and the associated email address.
- The application is a demonstration project and is not configured as a production hotel reservation platform.
- The postcard's weather information is illustrative and should not be treated as a live weather forecast.

## Future Improvements

- Add customer authentication and account management.
- Integrate a secure payment gateway.
- Support multiple room categories and room-level inventory.
- Add hotel and booking administration features.
- Integrate a live weather API.
- Add email notifications for booking confirmation and cancellation.
- Deploy the application with production-grade configuration and monitoring.
