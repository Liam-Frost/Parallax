package com.parallax.backend.auth;

import java.util.UUID;

public record AuthUserView(UUID id, String email, String displayName, String role) {
}
