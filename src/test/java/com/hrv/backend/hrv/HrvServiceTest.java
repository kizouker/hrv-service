package com.hrv.backend.hrv;

import com.hrv.backend.exception.ResourceNotFoundException;
import com.hrv.backend.hrv.dto.HrvReadingRequest;
import com.hrv.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HrvServiceTest {

    @Mock HrvRepository hrvRepository;
    @InjectMocks HrvService hrvService;

    private User user;
    private HrvReading reading;
    private HrvReadingRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("jane@example.com").build();

        reading = HrvReading.builder()
                .id(1L).user(user).measuredAt(Instant.now())
                .rmssd(55.3).sdnn(48.1).meanHr(73.0)
                .build();

        request = new HrvReadingRequest(
                null, Instant.now(),
                55.3, 48.1, 22.5, 820.0, 73.0,
                612.0, 1024.0, 0.60, "morning");
    }

    @Test
    void create_savesAndReturnsResponse() {
        when(hrvRepository.save(any())).thenReturn(reading);
        var response = hrvService.create(user, request);
        assertThat(response.rmssd()).isEqualTo(55.3);
        verify(hrvRepository).save(any(HrvReading.class));
    }

    @Test
    void getAll_returnsListForUser() {
        when(hrvRepository.findAllByUserIdOrderByMeasuredAtDesc(1L))
                .thenReturn(List.of(reading));
        var results = hrvService.getAll(user);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throws() {
        when(hrvRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hrvService.getById(user, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_callsRepository() {
        when(hrvRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(reading));
        hrvService.delete(user, 1L);
        verify(hrvRepository).delete(reading);
    }

    @Test
    void delete_wrongUser_throws() {
        when(hrvRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> hrvService.delete(user, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
