package com.staffsheet.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        String role
) {
    public static AuthResponse of(String token, Long userId, String email, String role) {
        return new AuthResponse(token, "Bearer", userId, email, role);
    }
}

