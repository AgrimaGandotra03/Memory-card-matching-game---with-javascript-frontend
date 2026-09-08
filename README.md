# Memory Card Matching Game — Backend

A simple Spring Boot REST API for a memory card matching game. Built as a BTech course prototype.

## Tech Stack

| Layer     | Technology                               |
|-----------|------------------------------------------|
| Language  | Java 17                                  |
| Framework | Spring Boot 3.3                          |
| Build     | Maven                                    |
| Database  | H2 (file-based, persists between runs)   |
| ORM       | Spring Data JPA / Hibernate              |
| Security  | BCryptPasswordEncoder only (no sessions) |

---

## Project Structure

```
src/main/java/com/memorygame/
├── MemoryGameApplication.java        <- entry point
├── config/
│   ├── CorsConfig.java               <- allow all origins for local dev
│   └── SecurityConfig.java           <- disables Spring Security HTTP filters; BCrypt bean
├── controller/
│   ├── AuthController.java           <- POST /api/auth/register & /login
│   ├── ProfileController.java        <- GET & PUT /api/profile/{userId}
│   └── GlobalExceptionHandler.java   <- turns all errors into JSON
├── dto/
│   ├── AuthRequest.java              <- {username, password} with validation
│   ├── AuthResponse.java             <- {userId, username, displayName, message}
│   ├── ErrorResponse.java            <- {error: "..."}
│   ├── ProfileResponse.java          <- full profile + stats
│   └── ProfileUpdateRequest.java     <- {displayName?, preferredTheme?}
├── model/
│   ├── User.java                     <- JPA entity: id, username, passwordHash, createdAt
│   └── PlayerProfile.java            <- JPA entity: linked to User, display name, stats
├── repository/
│   ├── UserRepository.java
│   └── PlayerProfileRepository.java
└── service/
    └── AuthService.java              <- register + login business logic
```

---

## Prerequisites

- Java 17+   — check with `java -version`
- Maven 3.6+ — check with `mvn -version`

---

## How to Run

```bash
# From the project root (where pom.xml is):
mvn spring-boot:run
```

The server starts at **http://localhost:8080**.

DevTools hot-reload is enabled: change a `.java` file, recompile with `mvn compile` in a second terminal (or Ctrl+F9 in IntelliJ), and the app restarts automatically.

---

## H2 Database Console

While the server is running, open: **http://localhost:8080/h2-console**

| Field    | Value                                             |
|----------|---------------------------------------------------|
| JDBC URL | `jdbc:h2:file:./data/memorygame;AUTO_SERVER=TRUE` |
| Username | `sa`                                              |
| Password | *(leave blank)*                                   |

The database file is at `./data/memorygame.mv.db` — data survives restarts.

---

## API Reference

### POST /api/auth/register

Create a new player account. Also creates a blank player profile automatically.

**Request body:**
```json
{ "username": "alice", "password": "secret123" }
```

**Success — 201 Created:**
```json
{
  "userId": 1,
  "username": "alice",
  "displayName": "alice",
  "message": "Registration successful. Welcome, alice!"
}
```

**Error — duplicate username (400):**
```json
{ "error": "Username 'alice' is already taken." }
```

**Error — validation failure (400):**
```json
{ "error": "password: Password must be at least 6 characters" }
```

---

### POST /api/auth/login

Log in with an existing account.

**Request body:**
```json
{ "username": "alice", "password": "secret123" }
```

**Success — 200 OK:**
```json
{
  "userId": 1,
  "username": "alice",
  "displayName": "alice",
  "message": "Login successful. Welcome back, alice!"
}
```

**Error — wrong credentials (401):**
```json
{ "error": "Invalid username or password." }
```

---

### GET /api/profile/{userId}

Fetch a player's profile and stats.

**Success — 200 OK:**
```json
{
  "userId": 1,
  "username": "alice",
  "displayName": "alice",
  "preferredTheme": null,
  "totalGamesPlayed": 0,
  "memberSince": "2024-03-15"
}
```

**Not found (404):**
```json
{ "error": "No profile found for userId 99" }
```

---

### PUT /api/profile/{userId}

Update display name and/or preferred theme. Only the fields you include are changed.

**Request body:**
```json
{ "displayName": "AliceWonderland", "preferredTheme": "animals" }
```

**Success — 200 OK:** returns the updated profile (same shape as GET).

---

## curl Test Commands

```bash
# 1. Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"secret123\"}"

# 2. Try registering the same username again (expect 400)
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"other\"}"

# 3. Login successfully
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"secret123\"}"

# 4. Login with wrong password (expect 401)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"alice\",\"password\":\"wrongpass\"}"

# 5. Get profile (replace 1 with your actual userId)
curl http://localhost:8080/api/profile/1

# 6. Update profile
curl -X PUT http://localhost:8080/api/profile/1 \
  -H "Content-Type: application/json" \
  -d "{\"displayName\":\"AliceWonderland\",\"preferredTheme\":\"animals\"}"

# 7. Verify the update
curl http://localhost:8080/api/profile/1
```

> **Windows PowerShell note:** Use Postman instead of curl on Windows — it's much easier than escaping quotes in PowerShell.

---

## Postman Quick Setup

1. Create a collection called **Memory Game API**
2. Base URL: `http://localhost:8080`

| Method | URL                | Body (raw JSON)                                                |
|--------|--------------------|----------------------------------------------------------------|
| POST   | /api/auth/register | `{"username":"alice","password":"secret123"}`                  |
| POST   | /api/auth/login    | `{"username":"alice","password":"secret123"}`                  |
| GET    | /api/profile/1     | —                                                              |
| PUT    | /api/profile/1     | `{"displayName":"AliceWonderland","preferredTheme":"animals"}` |

**Tip:** Save the `userId` from register into a Postman Environment Variable `{{userId}}` and use it in the profile URLs.

---

## What to Build Next

- `GameSession` entity — tracks cards and state for one game
- `POST /api/game/start` — creates a session with a shuffled card grid
- `POST /api/game/{sessionId}/flip` — flip a card, check for match
- `POST /api/game/{sessionId}/end` — finish the game, increment `totalGamesPlayed`
