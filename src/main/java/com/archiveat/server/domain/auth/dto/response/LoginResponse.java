package com.archiveat.server.domain.auth.dto.response;

public record LoginResponse (
        String accessToken,
        String grantType
) { }
