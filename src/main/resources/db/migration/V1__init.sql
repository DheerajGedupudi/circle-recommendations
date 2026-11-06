-- V1: schema for the Circle recommendations API.
--
-- Conventions:
--   * IDs come from the seed data, so primary keys are plain BIGINT (not generated).
--   * Topics are referenced by topics.id (slugs are translated to ids when loading).
--   * Fixed-choice fields are VARCHAR + CHECK: the database rejects bad values
--     from any source, and changing the allowed list is a one-line migration.
--   * Foreign keys use the default ON DELETE behaviour (block the delete), so no
--     row is ever removed by surprise.

-- ---------------------------------------------------------------------------
-- Topic taxonomy
-- ---------------------------------------------------------------------------
CREATE TABLE topics (
    id      BIGINT       PRIMARY KEY,
    slug    VARCHAR(100) NOT NULL UNIQUE,   -- UNIQUE also creates the index used to look topics up by slug
    label   VARCHAR(200) NOT NULL,
    domain  VARCHAR(50)  NOT NULL
);

-- ---------------------------------------------------------------------------
-- Members
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                    BIGINT       PRIMARY KEY,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    has_photo             BOOLEAN      NOT NULL,
    bio                   TEXT,
    job_title             VARCHAR(200),
    company               VARCHAR(200),
    industry              VARCHAR(100),
    location              VARCHAR(200),
    joined_at             TIMESTAMPTZ  NOT NULL,
    profile_completeness  NUMERIC(3,2) NOT NULL
        CHECK (profile_completeness BETWEEN 0 AND 1)
);

-- A member's stated topic interests (many-to-many: users <-> topics).
CREATE TABLE user_topic_interests (
    user_id   BIGINT NOT NULL REFERENCES users (id),
    topic_id  BIGINT NOT NULL REFERENCES topics (id),
    PRIMARY KEY (user_id, topic_id)   -- also serves "get this user's interests"
);

-- ---------------------------------------------------------------------------
-- Circles
-- ---------------------------------------------------------------------------
CREATE TABLE circles (
    id                 BIGINT       PRIMARY KEY,
    name               VARCHAR(200) NOT NULL,
    slug               VARCHAR(200) NOT NULL,
    description        TEXT,
    topic_id           BIGINT       REFERENCES topics (id),   -- NULL: some Circles have no topic
    cover_image_url    TEXT,
    format             VARCHAR(20)  NOT NULL CHECK (format IN ('in_person', 'virtual', 'hybrid')),
    access             VARCHAR(20)  NOT NULL CHECK (access IN ('public', 'unlisted')),
    join_policy        VARCHAR(20)  NOT NULL CHECK (join_policy IN ('open', 'request')),
    max_members        INTEGER      CHECK (max_members > 0),  -- NULL means no limit
    member_count       INTEGER      NOT NULL CHECK (member_count >= 0),  -- stored counter (read-heavy)
    created_at         TIMESTAMPTZ  NOT NULL,
    last_activity_at   TIMESTAMPTZ  NOT NULL,
    feed_posts_30d     INTEGER      NOT NULL CHECK (feed_posts_30d >= 0),
    chat_messages_30d  INTEGER      NOT NULL CHECK (chat_messages_30d >= 0)
);
-- "Is it full?" is not stored: it is derived from max_members and member_count.

-- Free-form tags (many-to-many: circles <-> topics). In this data every tag is a topic slug.
CREATE TABLE circle_tags (
    circle_id  BIGINT NOT NULL REFERENCES circles (id),
    topic_id   BIGINT NOT NULL REFERENCES topics (id),
    PRIMARY KEY (circle_id, topic_id)
);

-- ---------------------------------------------------------------------------
-- Memberships: who belongs to which Circle. Leaders are members with role = 'leader'.
-- ---------------------------------------------------------------------------
CREATE TABLE memberships (
    user_id    BIGINT      NOT NULL REFERENCES users (id),
    circle_id  BIGINT      NOT NULL REFERENCES circles (id),
    role       VARCHAR(20) NOT NULL CHECK (role IN ('member', 'leader')),
    joined_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, circle_id)   -- one membership per pair; also serves "this user's Circles"
);

-- ---------------------------------------------------------------------------
-- Circle feed posts
-- ---------------------------------------------------------------------------
CREATE TABLE posts (
    id              BIGINT      PRIMARY KEY,
    circle_id       BIGINT      NOT NULL REFERENCES circles (id),
    author_user_id  BIGINT      NOT NULL REFERENCES users (id),
    body            TEXT        NOT NULL,
    reaction_count  INTEGER     NOT NULL CHECK (reaction_count >= 0),
    comment_count   INTEGER     NOT NULL CHECK (comment_count >= 0),
    save_count      INTEGER     NOT NULL CHECK (save_count >= 0),
    created_at      TIMESTAMPTZ NOT NULL
);

-- ---------------------------------------------------------------------------
-- Indexes. Rule: add an index only if you can name the query it speeds up.
-- Primary keys and UNIQUE constraints are indexed automatically.
-- ---------------------------------------------------------------------------

-- "Who is in / who leads this Circle?" The primary key starts with user_id,
-- so it can't answer lookups by circle_id. Postgres doesn't index foreign keys automatically.
CREATE INDEX idx_memberships_circle_id ON memberships (circle_id);

-- At scale: find candidate Circles by topic instead of loading every Circle.
CREATE INDEX idx_circles_topic_id ON circles (topic_id);

-- At scale: count a Circle's posts in the last 30 days from real posts
-- (sorted by Circle, then date, so the range scan is direct).
CREATE INDEX idx_posts_circle_id_created_at ON posts (circle_id, created_at);

-- Deliberately not indexed (no query looks them up that way):
--   user_topic_interests (topic_id), circle_tags (topic_id), posts (author_user_id)
