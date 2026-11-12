# Transit & Tile

A fresher-level, full-stack hotel booking portal built with **Java 17 · Spring Boot 3.3.6 · MySQL 8 · React 19 · Bootstrap 5**.

---

## Prerequisites

| Tool | Required Version | Notes |
|------|-----------------|-------|
| Java JDK | 17+ | Project targets Java 17; tested on JDK 25.0.1 |
| Apache Maven | 3.9+ | Used to build and run the backend |
| MySQL Community Server | 8.0+ | Local instance; must be running before starting the backend |
| Node.js | 18+ | Required for the React frontend |
| npm | 9+ | Bundled with Node; used to install frontend dependencies |

---

## Environment setup

The backend reads all database credentials from environment variables.  
**Never put real credentials in any committed file.**

1. Copy the example file and fill in your local MySQL credentials:

   ```bash
   cp .env.example .env
   # Edit .env with your real values
   ```

2. Required environment variables:

   | Variable | Description | Default used if omitted |
   |----------|-------------|------------------------|
   | `DB_HOST` | MySQL host | `localhost` |
   | `DB_PORT` | MySQL port | `3306` |
   | `DB_NAME` | MySQL schema/database name | `transit_and_tile_db` |
   | `DB_USERNAME` | MySQL username | *(no default — required)* |
   | `DB_PASSWORD` | MySQL password | *(no default — required)* |

---

## Database setup

Before starting the backend, create the MySQL schema:

```sql
CREATE DATABASE IF NOT EXISTS transit_and_tile_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Log in to MySQL and run the above, or run:

```bash
mysql -u <your_username> -p -e "CREATE DATABASE IF NOT EXISTS transit_and_tile_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

> Table creation and sample data SQL scripts will be added in Phase 2.

---

## Running the backend

### Option A — PowerShell (Windows)

```powershell
$env:DB_USERNAME="your_mysql_username"
$env:DB_PASSWORD="your_mysql_password"
# Optional overrides:
# $env:DB_HOST="localhost"
# $env:DB_PORT="3306"
# $env:DB_NAME="transit_and_tile"

cd backend
mvn spring-boot:run
```

### Option B — Command Prompt (Windows)

```cmd
set DB_USERNAME=your_mysql_username
set DB_PASSWORD=your_mysql_password
cd backend
mvn spring-boot:run
```

The backend starts on **http://localhost:8081**.

---

## Running the frontend

```bash
cd frontend
npm install        # already done if node_modules exists
npm run dev
```

The frontend dev server starts on **http://localhost:5173**.

The Vite dev server proxies `/api` requests to `http://localhost:8081` automatically — no manual CORS configuration is needed during development.

---

## Running backend tests

```bash
cd backend
mvn test
```

The context-load smoke test uses H2 in-memory mode and does **not** require a running MySQL instance.

> MySQL integration tests (overlap queries, locking, concurrency) will be added in Phase 3–4 and require a live MySQL connection.

---

## Stack decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Spring Boot version | 3.3.6 | Latest stable release line compatible with Java 17+ |
| Java version target | 17 | Project specification requirement |
| MySQL connector | `com.mysql:mysql-connector-j` (managed by Spring Boot parent) | Recommended connector for Spring Boot 3 + MySQL 8 |
| Frontend bundler | Vite 8 | Pre-installed in the project; fast HMR |
| HTTP client | Browser Fetch API | Agreed in SKILL.md; no extra dependency |

---

## Project structure

```
TRANSIT_AND_TILE_workspace/
├── SKILL.md                  # Persistent engineering instructions
├── architecture.md           # Technical architecture blueprint
├── implementation_plan.md    # Phased implementation checklist
├── README.md                 # This file
├── DESIGN.md                 # (created during Phase 2+)
├── .gitignore
├── .env.example              # Safe credential placeholder
├── backend/
│   ├── pom.xml               # Maven project (Spring Boot 3.3.6, Java 17)
│   └── src/
│       ├── main/java/com/transitandtile/
│       │   └── TransitAndTileApplication.java
│       ├── main/resources/
│       │   └── application.properties
│       └── test/java/com/transitandtile/
│           └── TransitAndTileApplicationTests.java
└── frontend/
    ├── package.json          # React 19, Bootstrap 5, React Router 7, Vite 8
    ├── vite.config.js
    └── src/
        ├── App.jsx
        └── main.jsx
```

---

## Known limitations (Phase 1)

- No business endpoints exist yet (Phase 2 onwards).
- Schema creation SQL will be provided in Phase 2.
- MySQL integration tests are not yet written; the smoke test uses H2.
- Frontend shows the default Vite/React template page; real screens are built in Phase 6.
