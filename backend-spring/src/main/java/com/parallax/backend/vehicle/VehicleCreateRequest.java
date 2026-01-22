package com.parallax.backend.vehicle;

public record VehicleCreateRequest(String licenseNumber, String make, String model, Integer year) {
}
