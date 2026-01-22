package com.parallax.backend.vehicle;

import com.parallax.backend.common.ApiException;
import com.parallax.backend.security.CurrentUserService;
import com.parallax.backend.security.SecurityUtils;
import com.parallax.backend.user.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    public VehicleService(VehicleRepository vehicleRepository,
                          CurrentUserService currentUserService) {
        this.vehicleRepository = vehicleRepository;
        this.currentUserService = currentUserService;
    }

    public List<VehicleEntity> listVehicles() {
        UserEntity user = currentUserService.requireUser();
        if (SecurityUtils.hasRole("ADMIN")) {
            return vehicleRepository.findAll();
        }
        return vehicleRepository.findAllByOwnerId(user.getId());
    }

    public VehicleEntity addVehicle(VehicleCreateRequest request) {
        UserEntity user = currentUserService.requireUser();
        if (request == null || isBlank(request.licenseNumber()) || isBlank(request.make()) || isBlank(request.model())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Vehicle details are required");
        }

        String license = normalizePlate(request.licenseNumber());
        Optional<VehicleEntity> existing = vehicleRepository.findByLicenseNumberIgnoreCase(license);
        if (existing.isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "LICENSE_EXISTS", "License already exists");
        }

        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setOwner(user);
        vehicle.setLicenseNumber(license);
        vehicle.setMake(request.make().trim());
        vehicle.setModel(request.model().trim());
        vehicle.setYear(request.year());
        vehicle.setBlacklisted(false);
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(VehicleDeleteRequest request) {
        UserEntity user = currentUserService.requireUser();
        if (request == null || isBlank(request.licenseNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LICENSE_REQUIRED", "License is required");
        }
        String license = normalizePlate(request.licenseNumber());

        if (SecurityUtils.hasRole("ADMIN")) {
            VehicleEntity vehicle = vehicleRepository.findByLicenseNumberIgnoreCase(license)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Vehicle not found"));
            vehicleRepository.delete(vehicle);
            return;
        }

        VehicleEntity vehicle = vehicleRepository.findByOwnerIdAndLicenseNumberIgnoreCase(user.getId(), license)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Vehicle not found"));
        vehicleRepository.delete(vehicle);
    }

    public VehicleEntity updateBlacklist(BlacklistRequest request) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_ONLY", "Admin only");
        }
        if (request == null || isBlank(request.licenseNumber())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LICENSE_REQUIRED", "License is required");
        }
        String license = normalizePlate(request.licenseNumber());
        VehicleEntity vehicle = vehicleRepository.findByLicenseNumberIgnoreCase(license)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Vehicle not found"));
        vehicle.setBlacklisted(request.blacklisted());
        return vehicleRepository.save(vehicle);
    }

    public VehicleQueryResponse queryLicense(String license) {
        if (isBlank(license)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LICENSE_REQUIRED", "License is required");
        }
        String normalized = normalizePlate(license);
        Optional<VehicleEntity> match = vehicleRepository.findByLicenseNumberIgnoreCase(normalized);
        if (match.isEmpty()) {
            return new VehicleQueryResponse(true, false, normalized, false);
        }
        return new VehicleQueryResponse(true, true, normalized, match.get().isBlacklisted());
    }

    public VehicleOwnerView buildOwner(UserEntity owner) {
        if (owner == null) {
            return null;
        }
        String country = owner.getPhoneCountry() == null ? "" : owner.getPhoneCountry();
        String digits = owner.getPhoneDigits() == null ? "" : owner.getPhoneDigits();
        String phone = (country + digits).isBlank() ? null : country + digits;
        return new VehicleOwnerView(owner.getEmail(), phone);
    }

    private String normalizePlate(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
