# Design: Recommending Circles to Join

## What I built

A Spring Boot API, `GET /users/{id}/recommendations`, backed by Postgres. Flyway creates the schema
and loads the seed data, and Docker Compose runs it all with one command. The ranking is one plain Java
class (`Recommender`) with no framework or database code, so every rule is unit tested. The four
planted scenarios are end-to-end tests against a real Postgres.

## How a list is built

1. **Remove dead ends:** Circles the member is already in, **full** (`member_count >= max_members`, null = uncapped),
   **inactive** (no feed posts in 30 days), and **unlisted**. Request-to-join Circles stay, since that's one extra
   step, not a dead end. The reason says "The leader approves new members."
2. **Sort in three groups:** **stated** interest, then **inferred** interest (the topic of a Circle they're already in),
   then everything else. Within a group, the most posts in 30 days first; ties go to the lower id.
3. **Group near-duplicates:** same topic = duplicate (same-topic Circles here even share a description). The first
   per topic is the main pick, and the rest are attached as `alsoOnThisTopic`.
4. **Explain:** templates combining why it fits with an activity fact ("Matches your interest in Negotiation. 13 posts in the last 30 days.").
5. **Cold start:** with no stated interests, show the most active Circles across topics, and set `askForInterests: true`
   so the app asks. Asking beats guessing.

## Signals

| Signal | Use | Why |
|---|---|---|
| Topic vs. stated interests | Primary | The topic is why someone joins a Circle |
| Topics of Circles already joined | Secondary | A hint, so it ranks below what the member told us |
| `feed_posts_30d` | Ranking + inactive rule | Clearest activity signal, and it matches the posts table exactly |
| Capacity, `access` | Filters | Full or unlisted = dead end |
| Tags, profile fields, post text | Not used | Noisy or random in this data (10 Circles' tags omit their own topic) |

## Tradeoffs

- **Sort in steps, not a weighted score:** predictable, no made-up weights, every rank explainable. The cost: a quiet
  topic match always beats a lively non-match. A learned score needs outcome data we don't have.
- **Group duplicates, don't delete them:** the list stays clean, and the member can still choose by format
  (#1 is in person, #5 is hybrid). That's why user 7 gets #1 with #5 attached: #1 has more posts (15 against 11).
- **Inactive = 0 feed posts:** this catches exactly the dead Circles, but would miss a chat-only Circle. The data is
  ambiguous here: #21 reports 3 chats in 30 days but a last activity 227 days ago.
- **Unlisted excluded:** respects the leaders' choice, at the cost of one good match for user 15 (#30).
- **Rank all Circles in memory:** simple and testable at 40 Circles, but not how it scales (below).
- **Templates, not generated text:** reasons are always true and testable.
- **No clock:** the ranking uses the dataset's 30-day counts, so results don't drift after the snapshot date.

## Production scale (170,000+ Circles)

- **Candidate generation in SQL:** query only Circles on the member's topics (`circles(topic_id)` is indexed), with the
  filters in `WHERE` and a partial index on eligible Circles.
- **Precomputed activity:** compute 30-day posts and chat from events (`posts(circle_id, created_at)` is indexed) into a
  stats table on a schedule. Keep `member_count` in sync in the same transaction as join and leave.
- **Cache per member:** short TTL, invalidated when they join a Circle or change interests.
- **Spread demand:** every cold-start member sees the same top Circles, so rotate them and cap exposure per Circle.
- **Learn from outcomes:** log impressions and joins, A/B test (e.g. open Circles first for new members), then a learned score.

## With more time

Count chat in the inactive rule and give new Circles a grace period. Match in-person Circles by location
(no location field today). Mention leaders in reasons. Add a debug view of excluded Circles and why.
