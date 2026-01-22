package com.parallax.backend.vehicle;

public record BlacklistRequest(String licenseNumber, boolean blacklisted) {
}
