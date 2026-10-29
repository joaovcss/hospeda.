package com.hotelaria.dtos.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthenticatedUserResponse user
) {
}
