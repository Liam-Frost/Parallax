package com.parallax.backend.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<VehicleEntity, UUID> {
    Optional<VehicleEntity> findByLicenseNumberIgnoreCase(String licenseNumber);
    List<VehicleEntity> findAllByOwnerId(UUID ownerId);
    Optional<VehicleEntity> findByOwnerIdAndLicenseNumberIgnoreCase(UUID ownerId, String licenseNumber);
}
