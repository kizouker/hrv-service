package com.hrv.backend.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrv.backend.auth.AuthService;
import com.hrv.backend.auth.dto.RegisterRequest;
import com.hrv.backend.device.Device.DeviceType;
import com.hrv.backend.device.dto.DeviceRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;

    @Test
    void registerListAndDelete_device() throws Exception {
        String token = authService.register(
                new RegisterRequest("Device User", "device@hrv.com", "Str0ngP@ssword!")).token();

        var req = new DeviceRequest("My Polar H10", DeviceType.POLAR, "PH10-ABC123");
        MvcResult created = mockMvc.perform(post("/api/devices")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Polar H10"))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/devices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/devices/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/devices/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
