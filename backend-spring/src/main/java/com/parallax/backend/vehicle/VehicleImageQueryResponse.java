package com.parallax.backend.vehicle;

public record VehicleImageQueryResponse(
        boolean success,
        boolean plateFound,
        String licenseNumber,
        boolean foundInSystem,
        boolean blacklisted,
        Double confidence,
        String message
) {
}
