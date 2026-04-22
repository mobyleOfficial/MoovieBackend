# MoovieBackend

Backend API for the Moovie app, powered by [TMDB](https://www.themoviedb.org/). Built with Kotlin, Ktor, and Koin.

## Tech Stack

- **Kotlin** 2.0.20
- **Ktor** 2.3.0 (Server + Client)
- **Koin** 3.5.6 (Dependency Injection)
- **Kotlinx Serialization** (JSON)
- **Netty** (HTTP engine)
- **Java** 17+

## Prerequisites

- JDK 17 or higher
- A [TMDB API](https://developer.themoviedb.org/) Bearer token

## Environment Variables

| Variable       | Required | Default | Description                                      |
|----------------|----------|---------|--------------------------------------------------|
| `TMDB_API_KEY` | Yes      | —       | TMDB API Bearer token                            |
| `PORT`         | No       | `8080`  | Server port                                      |
| `CORS_ORIGINS` | No       | —       | Comma-separated list of allowed origins for CORS |

## Getting Started

### Run locally

```bash
export TMDB_API_KEY="your_tmdb_bearer_token"
./gradlew run
```

The server starts at `http://localhost:8080`.

### Build fat JAR

```bash
./gradlew buildFatJar
java -jar build/libs/MoovieBackend-1.0-SNAPSHOT-all.jar
```

### Docker

```bash
docker build -t moovie-backend .
docker run -p 8080:8080 \
  -e TMDB_API_KEY="your_tmdb_bearer_token" \
  moovie-backend
```

## API Endpoints

### Movies

| Method | Endpoint                      | Description              | Query Params                                                                                                        |
|--------|-------------------------------|--------------------------|---------------------------------------------------------------------------------------------------------------------|
| GET    | `/movies/trending`            | Trending movies          | `page`                                                                                                              |
| GET    | `/movies/search`              | Search movies            | `query` (required), `page`                                                                                          |
| GET    | `/movies/discover`            | Discover with filters    | `page`, `year`, `release_date_gte`, `release_date_lte`, `sort_by`, `with_genres`, `with_original_language`, `with_origin_country`, `vote_count_gte` |
| GET    | `/movies/genres`              | List genres              | —                                                                                                                   |
| GET    | `/movies/countries`           | List countries           | —                                                                                                                   |
| GET    | `/movies/languages`           | List languages           | —                                                                                                                   |
| GET    | `/movies/{id}`                | Movie details            | —                                                                                                                   |
| GET    | `/movies/{id}/reviews`        | Movie reviews            | `page`                                                                                                              |
| GET    | `/movies/favorites/{userId}`  | User's favorite movies   | `page`                                                                                                              |
| GET    | `/movies/watchlist/{userId}`  | User's watchlist         | `page`                                                                                                              |
| GET    | `/movies/lists`               | All movie lists          | `page`, `userId`                                                                                                    |
| GET    | `/movies/lists/user`          | Current user's lists     | `page`                                                                                                              |
| GET    | `/movies/lists/featured`      | Featured lists           | `page`                                                                                                              |
| GET    | `/movies/lists/{id}`          | List details             | `page`                                                                                                              |

### Profile

| Method | Endpoint              | Description          | Body           |
|--------|-----------------------|----------------------|----------------|
| GET    | `/profile`            | Get current profile  | —              |
| PUT    | `/profile`            | Update profile       | `UserProfile`  |
| GET    | `/profile/{userId}`   | Get public profile   | —              |

### Activities

| Method | Endpoint                 | Description            | Body               |
|--------|--------------------------|------------------------|---------------------|
| GET    | `/activities/{userId}`   | User's activities      | —                   |
| GET    | `/activities/friends`    | Friends' activities    | `page`              |
| POST   | `/reviews`               | Submit a movie review  | `MovieReviewDraft`  |

## Project Structure

```
src/main/kotlin/org/mobyle/
├── Main.kt                         # Entry point & server config
├── routing/                        # Route definitions
│   ├── MoviesRouting.kt
│   ├── ProfileRouting.kt
│   └── ActivitiesRouting.kt
├── di/                             # Koin modules
│   ├── AppModule.kt
│   └── Utils.kt
├── domain/
│   ├── model/                      # Domain models
│   ├── repository/                 # Repository interfaces
│   └── usecase/                    # Use cases (movies, profile, activities)
├── data/
│   ├── di/                         # Data layer DI
│   ├── remote/                     # TMDB API client & mappers
│   └── repository/                 # Repository implementations
└── model/                          # Response/listing models
```

## CORS

By default, `localhost:5173` and `127.0.0.1:5173` are allowed for local development. Add extra origins via the `CORS_ORIGINS` env var:

```bash
export CORS_ORIGINS="https://myapp.com,https://staging.myapp.com"
```

## Deployment

The included `Dockerfile` uses a multi-stage build (JDK 17 Alpine) and runs as a non-root user. Compatible with Railway, Fly.io, Render, or any container platform — just set the `TMDB_API_KEY` environment variable.
