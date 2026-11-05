package com.hrv.backend.hrv;

import com.hrv.backend.auth.AuthService;
import com.hrv.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HrvInfoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;

    private String token;

    @BeforeEach
    void setUp() {
        // H2 is shared across tests: unique email per registration.
        token = authService.register(new RegisterRequest("Info User",
                "info" + System.nanoTime() + "@hrv.com", "Str0ngP@ssword!")).token();
    }

    @Test
    void overview_returnsTopics() throws Exception {
        mockMvc.perform(get("/api/hrv/info").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.what_is_hrv").isNotEmpty())
                .andExpect(jsonPath("$.topics").isArray());
    }

    @Test
    void metrics_returnsAllMetrics() throws Exception {
        mockMvc.perform(get("/api/hrv/info/metrics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("RMSSD"))
                .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void breathwork_returnsProtocols() throws Exception {
        mockMvc.perform(get("/api/hrv/info/breathwork").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Resonance / Coherence Breathing"));
    }

    @Test
    void protocols_returnsMeasurementProtocols() throws Exception {
        mockMvc.perform(get("/api/hrv/info/protocols").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Morning Baseline (recommended daily)"));
    }

    @Test
    void valsalva_returnsPhases() throws Exception {
        mockMvc.perform(get("/api/hrv/info/valsalva").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Valsalva Maneuver"))
                .andExpect(jsonPath("$.phases").isArray());
    }
}
