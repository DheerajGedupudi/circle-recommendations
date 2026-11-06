# Circle Recommendations API

An API that returns a **ranked list of Lean In Circles a member should join**, each with a short,
member-facing reason, built on the provided seed dataset.

- **Design doc (1 page):** [docs/DESIGN.md](docs/DESIGN.md): what I built, the tradeoffs, and what I'd add for production scale
- **Database schema:** [docs/SCHEMA.md](docs/SCHEMA.md): tables, relationships, and indexing decisions

**Stack:** Java 25 · Spring Boot 4.1 · PostgreSQL 18 · Flyway · JPA/Hibernate · Testcontainers · Docker Compose

---

## Quick start

The only requirement is **Docker** (Docker Desktop or similar).

```bash
docker compose up --build
```

This builds the app, starts Postgres, creates the schema, and loads the seed data. The API is ready
when the log shows `Started CircleRecommendationsApplication`.

- API: http://localhost:8080
- Postgres (optional, for inspection): `localhost:5433`, database / user / password `circles`

Stop with `Ctrl+C`, then `docker compose down`. That removes the containers, so the next start loads
fresh seed data.

## Try the planted scenarios

```bash
curl -s localhost:8080/users/42/recommendations   # Cold start: no interests, no memberships
curl -s localhost:8080/users/7/recommendations    # Obvious matches: leadership + public speaking
curl -s localhost:8080/users/15/recommendations   # Duplicates: three near-identical negotiation Circles
curl -s localhost:8080/users/23/recommendations   # Full / inactive: #20 is full, #21 is inactive
```

| User | What you'll see |
|---|---|
| **42** (cold start) | The most active Circles, one per topic, all `popular`, and `askForInterests: true` so the app can prompt her to pick interests |
| **7** (obvious matches) | #1 The Elevation Circle (leadership, with **#5 Women Who Lead** attached) and **#6 Speak Up Circle** first |
| **15** (duplicates) | Exactly **one** negotiation pick (#11). #12 and #10 are attached under it, not repeated |
| **23** (full / inactive) | **#22 Level Up Collective** first. #20 (full) and #21 (inactive) never appear |

Every one of the 100 users gets a non-empty list.

## API

### `GET /users/{userId}/recommendations?limit=5`

| Parameter | Default | Notes |
|---|---|---|
| `limit` | `5` | Number of main picks, 1–20 |

Example (`/users/15/recommendations?limit=2`):

```json
{
  "userId": 15,
  "askForInterests": false,
  "recommendations": [
    {
      "rank": 1,
      "circleId": 13,
      "name": "Confident Voices",
      "topic": "Confidence",
      "matchType": "stated_interest",
      "format": "in_person",
      "joinPolicy": "open",
      "memberCount": 12,
      "maxMembers": 18,
      "postsLast30Days": 14,
      "reason": "Matches your interest in Confidence. 14 posts in the last 30 days.",
      "alsoOnThisTopic": [
        { "circleId": 37, "name": "The Confidence Lab", "format": "virtual" }
      ]
    },
    {
      "rank": 2,
      "circleId": 11,
      "name": "The Negotiation Table",
      "topic": "Negotiation",
      "matchType": "stated_interest",
      "format": "hybrid",
      "joinPolicy": "open",
      "memberCount": 15,
      "maxMembers": 25,
      "postsLast30Days": 13,
      "reason": "Matches your interest in Negotiation. 13 posts in the last 30 days.",
      "alsoOnThisTopic": [
        { "circleId": 12, "name": "Negotiate Like a Leader", "format": "in_person" },
        { "circleId": 10, "name": "Negotiation Circle", "format": "in_person" }
      ]
    }
  ]
}
```

- `matchType`: `stated_interest` · `inferred_interest` (topic of a Circle the member already belongs to) · `popular`
- `alsoOnThisTopic`: other eligible Circles on the same topic, so the member can still choose, for example by format
- `askForInterests`: `true` when the member has no stated interests

**Errors** use the standard [problem details](https://www.rfc-editor.org/rfc/rfc9457) format:

| Case | Status |
|---|---|
| Unknown user | `404` with `"detail": "User 9999 not found"` |
| `limit` outside 1–20 | `400` |
| No eligible Circles | `200` with an empty list |

## Running the tests

Requires **JDK 25** and **Docker running** (the tests start a real Postgres with Testcontainers).

```bash
./mvnw test
```

- `RecommenderTest`: unit tests for every ranking rule (filters, ordering, tie-break, duplicate grouping, reasons). No Spring or database.
- `RecommendationsApiTest`: end-to-end tests for the four planted scenarios plus the error cases, against Postgres loaded by the same migrations the app uses.

## Project layout

```
src/main/java/com/dheeraj/circle_recommendations/
  controller/   RecommendationController, response records
  service/      Recommender (the ranking logic, plain Java), RecommendationService (loads data)
  repository/   Spring Data JPA repositories
  entity/       JPA entities + string-backed enum converters
src/main/java/db/seed/V2__Seed_data.java   Flyway Java migration that loads the seed JSON
src/main/resources/db/migration/V1__init.sql   Schema
src/main/resources/seed/   The provided dataset
```

## How I worked with AI

I used **Claude Code** throughout, and the commits it helped write are marked `Co-Authored-By: Claude`.

- **Understanding the data:** it profiled the dataset with me and surfaced issues the README doesn't
  mention. For example, Circle #21 reports chat messages in the last 30 days but a last activity
  ~7 months ago, Circle #17 lists one tag twice, and tags and post text are mostly random.
- **Making decisions:** for each design question (filters, ranking, duplicates, cold start, schema,
  indexes), I had it lay out the options and tradeoffs, checked each against the planted users, and
  made the call myself. Those decisions and their rationale are in [docs/DESIGN.md](docs/DESIGN.md).
- **Building:** once the plan was fixed, Claude implemented it step by step. Each step was verified
  for real (tests, `docker compose up`, calling the API for all 100 users) before being committed.
