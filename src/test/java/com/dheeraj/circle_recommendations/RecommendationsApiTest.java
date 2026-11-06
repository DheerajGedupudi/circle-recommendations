package com.dheeraj.circle_recommendations;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end tests for the four planted scenarios, against a real Postgres (Testcontainers)
 * loaded by the same Flyway migrations the app uses.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class RecommendationsApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void coldStartUserGetsActiveCirclesAcrossTopicsAndIsAskedForInterests() throws Exception {
        mvc.perform(get("/users/42/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.askForInterests").value(true))
            .andExpect(jsonPath("$.recommendations[*].circleId").value(contains(3, 27, 15, 1, 13)))
            .andExpect(jsonPath("$.recommendations[*].matchType").value(everyItem(is("popular"))));
    }

    @Test
    void obviousMatchesComeFirst() throws Exception {
        mvc.perform(get("/users/7/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.askForInterests").value(false))
            // Leadership: #1 is most active; #5 Women Who Lead is attached to it.
            .andExpect(jsonPath("$.recommendations[0].circleId").value(1))
            .andExpect(jsonPath("$.recommendations[0].matchType").value("stated_interest"))
            .andExpect(jsonPath("$.recommendations[0].alsoOnThisTopic[*].circleId").value(hasItem(5)))
            // Public speaking: #6 Speak Up Circle.
            .andExpect(jsonPath("$.recommendations[1].circleId").value(6))
            .andExpect(jsonPath("$.recommendations[1].matchType").value("stated_interest"));
    }

    @Test
    void nearDuplicateCirclesAreGroupedNotRepeated() throws Exception {
        mvc.perform(get("/users/15/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[*].circleId").value(contains(13, 11, 1, 34, 3)))
            // Exactly one negotiation main pick; the other two are attached to it.
            .andExpect(jsonPath("$.recommendations[?(@.topic == 'Negotiation')].circleId").value(contains(11)))
            .andExpect(jsonPath("$.recommendations[1].alsoOnThisTopic[*].circleId").value(contains(12, 10)));
    }

    @Test
    void fullAndInactiveCirclesNeverAppear() throws Exception {
        mvc.perform(get("/users/23/recommendations"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[0].circleId").value(22))
            .andExpect(jsonPath("$.recommendations[0].matchType").value("stated_interest"))
            .andExpect(jsonPath("$.recommendations[0].reason").value(
                "Matches your interest in Getting promoted. 8 posts in the last 30 days."
                    + " The leader approves new members."))
            // #20 (full) and #21 (inactive) appear nowhere, not even as "also on this topic".
            .andExpect(jsonPath("$..circleId").value(not(hasItems(20))))
            .andExpect(jsonPath("$..circleId").value(not(hasItems(21))));
    }

    @Test
    void limitControlsHowManyPicksAreReturned() throws Exception {
        mvc.perform(get("/users/23/recommendations").param("limit", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recommendations[*].circleId").value(contains(22)));
    }

    @Test
    void unknownUserIs404() throws Exception {
        mvc.perform(get("/users/9999/recommendations"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.detail").value("User 9999 not found"));
    }

    @Test
    void limitOutsideOneToTwentyIs400() throws Exception {
        mvc.perform(get("/users/7/recommendations").param("limit", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/users/7/recommendations").param("limit", "21"))
            .andExpect(status().isBadRequest());
    }
}
