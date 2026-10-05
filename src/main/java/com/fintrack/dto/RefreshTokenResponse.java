package com.fintrack.dto;

public class RefreshTokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;

    public RefreshTokenResponse() {
    }

    public RefreshTokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }
}