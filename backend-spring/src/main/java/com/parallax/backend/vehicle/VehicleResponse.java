package com.parallax.backend.vehicle;

public record VehicleResponse(
        String licenseNumber,
        String make,
        String model,
        Integer year,
        boolean blacklisted,
        VehicleOwnerView owner
) {
    public static VehicleResponse from(VehicleEntity vehicle, VehicleOwnerView owner) {
        return new VehicleResponse(
                vehicle.getLicenseNumber(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.isBlacklisted(),
                owner
        );
    }
}
