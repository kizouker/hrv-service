package com.hrv.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrv.backend.auth.dto.ChangePasswordRequest;
import com.hrv.backend.auth.dto.LoginRequest;
import com.hrv.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAccountTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AuthService authService;

    @Test
    void changePassword_thenLoginWithNewPassword() throws Exception {
        String token = authService.register(
                new RegisterRequest("Pw User", "pwchange@hrv.com", "Str0ngP@ssword!")).token();

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Str0ngP@ssword!", "An0therStr0ng!"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("pwchange@hrv.com", "An0therStr0ng!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        String token = authService.register(
                new RegisterRequest("Pw User", "pweak@hrv.com", "Str0ngP@ssword!")).token();

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Str0ngP@ssword!", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.newPassword").exists());

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("Str0ngP@ssword!", "alllowercase1!"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.newPassword").exists());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("pweak@hrv.com", "Str0ngP@ssword!"))))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongCurrent_returns400() throws Exception {
        String token = authService.register(
                new RegisterRequest("Pw User", "pwrong@hrv.com", "Str0ngP@ssword!")).token();

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordRequest("WrongPassword1!", "An0therStr0ng!"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteAccount_thenLoginFails() throws Exception {
        String email = "deleteMe@hrv.com".toLowerCase();
        String token = authService.register(
                new RegisterRequest("Delete Me", email, "Str0ngP@ssword!")).token();

        mockMvc.perform(delete("/api/auth/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "Str0ngP@ssword!"))))
                .andExpect(status().isUnauthorized());
    }
}
