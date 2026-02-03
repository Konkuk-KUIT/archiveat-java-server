package com.archiveat.server.global.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 50000, "서버 내부 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, 40000, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 40100, "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 40300, "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, 40400, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, 40500, "허용되지 않은 Http 메서드입니다."),

    // [Explore & Inbox]
    USER_NEWSLETTER_NOT_FOUND(HttpStatus.NOT_FOUND, 40410, "인박스 아이템을 찾을 수 없습니다."),
    USER_NEWSLETTER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 40310, "해당 인박스 아이템에 대한 수정 권한이 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, 40411, "존재하지 않는 카테고리입니다."),
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, 40412, "존재하지 않는 토픽입니다."),

    // Collection
    COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "컬렉션을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
