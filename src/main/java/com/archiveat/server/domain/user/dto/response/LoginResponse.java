package com.archiveat.server.domain.user.dto.response;

public record LoginResponse (
        String accessToken,
        String grantType
) { }
