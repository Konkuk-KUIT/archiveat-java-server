package com.archiveat.server.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {
    SUCCESS(HttpStatus.OK, 20000, "Success"),
    USER_CREATED(HttpStatus.OK, 20000, "User created");


    private final HttpStatus status;
    private final int Code;
    private final String message;
}
