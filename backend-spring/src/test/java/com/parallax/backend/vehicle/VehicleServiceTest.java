package com.parallax.backend.vehicle;

import com.parallax.backend.common.ApiException;
import com.parallax.backend.security.CurrentUserService;
import com.parallax.backend.user.UserEntity;
import com.parallax.backend.user.UserRole;
import com.parallax.backend.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {
    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    CurrentUserService currentUserService;

    @Captor
    ArgumentCaptor<VehicleEntity> vehicleCaptor;

    VehicleService vehicleService;

    @BeforeEach
    void setup() {
        vehicleService = new VehicleService(vehicleRepository, currentUserService);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addVehicle_normalizesPlateAndSaves() {
        UserEntity user = baseUser();
        when(currentUserService.requireUser()).thenReturn(user);
        when(vehicleRepository.findByLicenseNumberIgnoreCase("ABC123")).thenReturn(Optional.empty());
        doAnswer(invocation -> invocation.getArgument(0)).when(vehicleRepository).save(any(VehicleEntity.class));

        vehicleService.addVehicle(new VehicleCreateRequest("abc123", "Toyota", "Corolla", 2020));

        verify(vehicleRepository).save(vehicleCaptor.capture());
        assertThat(vehicleCaptor.getValue().getLicenseNumber()).isEqualTo("ABC123");
        assertThat(vehicleCaptor.getValue().getOwner().getId()).isEqualTo(user.getId());
    }

    @Test
    void listVehicles_userOnlyQueriesOwn() {
        UserEntity user = baseUser();
        when(currentUserService.requireUser()).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getId().toString(), "t",
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        vehicleService.listVehicles();
        verify(vehicleRepository).findAllByOwnerId(user.getId());
        verify(vehicleRepository, never()).findAll();
    }

    @Test
    void updateBlacklist_deniesNonAdmin() {
        UserEntity user = baseUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getId().toString(), "t",
                        List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        assertThatThrownBy(() -> vehicleService.updateBlacklist(new BlacklistRequest("ABC123", true)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Admin only");
    }

    private UserEntity baseUser() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
