package com.parallax.backend.vehicle;

public record VehicleQueryResponse(boolean success, boolean found, String licenseNumber, boolean blacklisted) {
}
