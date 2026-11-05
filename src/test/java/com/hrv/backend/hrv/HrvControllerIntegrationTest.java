package com.hrv.backend.hrv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrv.backend.auth.AuthService;
import com.hrv.backend.auth.dto.RegisterRequest;
import com.hrv.backend.hrv.dto.HrvReadingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HrvControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;

    private int userSeq;

    private String register() {
        String email = "hrv" + (++userSeq) + System.nanoTime() + "@hrv.com";
        return authService.register(
                new RegisterRequest("Hrv User", email, "Str0ngP@ssword!")).token();
    }

    private HrvReadingRequest reading(Instant at) {
        return new HrvReadingRequest(null, at,
                55.3, 48.1, 22.5, 820.0, 73.0,
                612.0, 1024.0, 0.60, "morning");
    }

    @Test
    void createAndGet_reading() throws Exception {
        String token = register();

        MvcResult created = mockMvc.perform(post("/api/hrv")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reading(Instant.now()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rmssd").value(55.3))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/hrv").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/hrv/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getById_missing_returns404() throws Exception {
        String token = register();
        mockMvc.perform(get("/api/hrv/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAndDelete_reading() throws Exception {
        String token = register();

        MvcResult created = mockMvc.perform(post("/api/hrv")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reading(Instant.now()))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        var updated = new HrvReadingRequest(null, Instant.now(),
                60.0, 48.1, 22.5, 820.0, 73.0,
                null, null, null, "evening");
        mockMvc.perform(put("/api/hrv/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rmssd").value(60.0));

        mockMvc.perform(delete("/api/hrv/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/hrv/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_invalid_returns400() throws Exception {
        String token = register();
        var invalid = new HrvReadingRequest(null, null,
                55.3, 48.1, 22.5, 820.0, 73.0, null, null, null, null);
        mockMvc.perform(post("/api/hrv")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void summaryAndTrend_returnStats() throws Exception {
        String token = register();
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/hrv")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    reading(Instant.now().minusSeconds(i * 3600)))))
                    .andExpect(status().isCreated());
        }

        MvcResult summary = mockMvc.perform(get("/api/hrv/analytics/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingCount").value(2))
                .andReturn();
        JsonNode node = objectMapper.readTree(summary.getResponse().getContentAsString());
        assertThat(node.get("avgRmssd").asDouble()).isEqualTo(55.3);

        mockMvc.perform(get("/api/hrv/analytics/trend")
                        .header("Authorization", "Bearer " + token)
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].avgRmssd").value(55.3));
    }

    @Test
    void summary_noReadings_returnsZero() throws Exception {
        String token = register();
        mockMvc.perform(get("/api/hrv/analytics/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readingCount").value(0));
    }
}
