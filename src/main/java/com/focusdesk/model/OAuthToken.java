package com.focusdesk.model;

public record OAuthToken(
        int userId,
        String provider,
        String accessToken,
        String refreshToken,
        String tokenType,
        String scope,
        String expiresAt
) {}
