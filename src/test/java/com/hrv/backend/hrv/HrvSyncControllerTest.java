package com.hrv.backend.hrv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrv.backend.auth.AuthService;
import com.hrv.backend.auth.dto.RegisterRequest;
import com.hrv.backend.hrv.dto.HealthKitSyncRequest;
import com.hrv.backend.hrv.dto.HrvReadingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HrvSyncControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;

    private String register(String email) {
        return authService.register(
                new RegisterRequest("Sync User", email, "Str0ngP@ssword!")).token();
    }

    private HealthKitSyncRequest syncRequest(Instant base) {
        return new HealthKitSyncRequest("Apple Watch", "device-1", List.of(
                new HrvReadingRequest(null, base, 55.0, 48.0, 20.0, 820.0, 73.0,
                        null, null, null, "sync"),
                new HrvReadingRequest(null, base.plusSeconds(60), 57.0, 49.0, 21.0, 815.0, 72.0,
                        null, null, null, "sync")));
    }

    @Test
    void status_noReadings_returnsFalse() throws Exception {
        String token = register("syncfresh@hrv.com");
        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReadings").value(false));
    }

    @Test
    void sync_savesThenSkipsDuplicates() throws Exception {
        String token = register("syncuser@hrv.com");
        String body = objectMapper.writeValueAsString(syncRequest(Instant.now()));

        mockMvc.perform(post("/api/sync/healthkit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.saved").value(2))
                .andExpect(jsonPath("$.skippedDuplicates").value(0));

        mockMvc.perform(post("/api/sync/healthkit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(0))
                .andExpect(jsonPath("$.skippedDuplicates").value(2));

        mockMvc.perform(get("/api/sync/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasReadings").value(true));
    }
}
