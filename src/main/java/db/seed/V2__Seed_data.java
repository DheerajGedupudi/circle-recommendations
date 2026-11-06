package db.seed;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads the provided seed dataset (classpath:seed/*.json) into the tables created by V1.
 *
 * <ul>
 *   <li>Flyway runs it exactly once, in every environment that includes classpath:db/seed
 *       (local, Docker, tests). The "prod" profile leaves that location out, so demo data
 *       never reaches a production database.</li>
 *   <li>It uses plain JDBC, not JPA entities: a migration must keep working unchanged even
 *       after the entity classes evolve.</li>
 *   <li>Topic slugs are translated to topic ids. Duplicate tags are dropped (Circle 17 lists
 *       one tag twice in the source data; the circle_tags primary key would reject it).</li>
 * </ul>
 */
public class V2__Seed_data extends BaseJavaMigration {

    private final JsonMapper json = new JsonMapper();

    @Override
    public void migrate(Context context) throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));

        // Parents before children, so every foreign key already exists when it's referenced.
        Map<String, Long> topicIdBySlug = loadTopics(jdbc);
        loadUsers(jdbc, topicIdBySlug);
        loadCircles(jdbc, topicIdBySlug);
        loadMemberships(jdbc);
        loadPosts(jdbc);
    }

    private Map<String, Long> loadTopics(JdbcTemplate jdbc) throws IOException {
        Map<String, Long> topicIdBySlug = new HashMap<>();
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode t : read("topics.json").values()) {
            long id = t.get("id").asLong();
            String slug = text(t, "slug");
            topicIdBySlug.put(slug, id);
            rows.add(new Object[] {id, slug, text(t, "label"), text(t, "domain")});
        }
        jdbc.batchUpdate("INSERT INTO topics (id, slug, label, domain) VALUES (?, ?, ?, ?)", rows);
        return topicIdBySlug;
    }

    private void loadUsers(JdbcTemplate jdbc, Map<String, Long> topicIdBySlug) throws IOException {
        List<Object[]> users = new ArrayList<>();
        List<Object[]> interests = new ArrayList<>();
        for (JsonNode u : read("users.json").values()) {
            long id = u.get("id").asLong();
            users.add(new Object[] {
                id, text(u, "first_name"), text(u, "last_name"), u.get("has_photo").asBoolean(),
                text(u, "bio"), text(u, "job_title"), text(u, "company"), text(u, "industry"),
                text(u, "location"), time(u, "joined_at"), decimal(u, "profile_completeness")
            });
            for (String slug : distinctStrings(u.get("topic_interests"))) {
                interests.add(new Object[] {id, topicId(topicIdBySlug, slug)});
            }
        }
        jdbc.batchUpdate("""
            INSERT INTO users (id, first_name, last_name, has_photo, bio, job_title, company,
                               industry, location, joined_at, profile_completeness)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", users);
        jdbc.batchUpdate("INSERT INTO user_topic_interests (user_id, topic_id) VALUES (?, ?)", interests);
    }

    private void loadCircles(JdbcTemplate jdbc, Map<String, Long> topicIdBySlug) throws IOException {
        List<Object[]> circles = new ArrayList<>();
        List<Object[]> tags = new ArrayList<>();
        for (JsonNode c : read("circles.json").values()) {
            long id = c.get("id").asLong();
            String topicSlug = text(c, "topic");
            circles.add(new Object[] {
                id, text(c, "name"), text(c, "slug"), text(c, "description"),
                topicSlug == null ? null : topicId(topicIdBySlug, topicSlug),
                text(c, "cover_image_url"), text(c, "format"), text(c, "access"), text(c, "join_policy"),
                nullableInt(c, "max_members"), c.get("member_count").asInt(),
                time(c, "created_at"), time(c, "last_activity_at"),
                c.get("feed_posts_30d").asInt(), c.get("chat_messages_30d").asInt()
            });
            for (String slug : distinctStrings(c.get("tags"))) {
                tags.add(new Object[] {id, topicId(topicIdBySlug, slug)});
            }
        }
        jdbc.batchUpdate("""
            INSERT INTO circles (id, name, slug, description, topic_id, cover_image_url, format, access,
                                 join_policy, max_members, member_count, created_at, last_activity_at,
                                 feed_posts_30d, chat_messages_30d)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", circles);
        jdbc.batchUpdate("INSERT INTO circle_tags (circle_id, topic_id) VALUES (?, ?)", tags);
    }

    private void loadMemberships(JdbcTemplate jdbc) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode m : read("memberships.json").values()) {
            rows.add(new Object[] {
                m.get("user_id").asLong(), m.get("circle_id").asLong(), text(m, "role"), time(m, "joined_at")
            });
        }
        jdbc.batchUpdate("INSERT INTO memberships (user_id, circle_id, role, joined_at) VALUES (?, ?, ?, ?)", rows);
    }

    private void loadPosts(JdbcTemplate jdbc) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (JsonNode p : read("activity.json").values()) {
            rows.add(new Object[] {
                p.get("id").asLong(), p.get("circle_id").asLong(), p.get("author_user_id").asLong(),
                text(p, "body"), p.get("reaction_count").asInt(), p.get("comment_count").asInt(),
                p.get("save_count").asInt(), time(p, "created_at")
            });
        }
        jdbc.batchUpdate("""
            INSERT INTO posts (id, circle_id, author_user_id, body, reaction_count, comment_count,
                               save_count, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", rows);
    }

    // ---- small JSON helpers ----------------------------------------------------------------

    private JsonNode read(String file) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("seed/" + file)) {
            if (in == null) {
                throw new IllegalStateException("Seed file not found on classpath: seed/" + file);
            }
            return json.readTree(in);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static Integer nullableInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.get(field).decimalValue();
    }

    private static OffsetDateTime time(JsonNode node, String field) {
        return OffsetDateTime.parse(node.get(field).asString());
    }

    private static List<String> distinctStrings(JsonNode array) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode v : array.values()) {
            values.add(v.asString());
        }
        return List.copyOf(values);
    }

    private static long topicId(Map<String, Long> topicIdBySlug, String slug) {
        Long id = topicIdBySlug.get(slug);
        if (id == null) {
            throw new IllegalStateException("Unknown topic slug in seed data: " + slug);
        }
        return id;
    }
}
