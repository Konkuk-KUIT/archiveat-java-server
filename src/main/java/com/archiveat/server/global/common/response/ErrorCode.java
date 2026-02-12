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

    // [Auth Domain]
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, 40105, "이메일 또는 비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, 40901, "이미 존재하는 이메일입니다."),
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, 40106, "리프레시 토큰이 없습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, 40107, "유효하지 않은 리프레시 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, 40108, "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, 40301, "접근 권한이 없습니다."),
    INVALID_TOKEN_SIGNATURE(HttpStatus.UNAUTHORIZED, 40109, "유효하지 않은 토큰 서명입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, 40110, "지원하지 않는 토큰 형식입니다."),
    LOGOUT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50002, "로그아웃 처리에 실패했습니다."),

    // [User Domain]
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 40402, "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, 40902, "이미 존재하는 사용자입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, 40903, "이미 존재하는 닉네임입니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, 40011, "닉네임 형식이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, 40014, "현재 비밀번호가 일치하지 않습니다."),
    SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST, 40015, "새 비밀번호는 기존 비밀번호와 다르게 설정해야 합니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, 40302, "탈퇴한 회원입니다."),

    // [Newsletter Domain]
    NEWSLETTER_NOT_FOUND(HttpStatus.NOT_FOUND, 40413, "뉴스레터를 찾을 수 없습니다."),
    NEWSLETTER_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "뉴스레터 처리 중 오류가 발생했습니다."),
    INVALID_CONTENT_URL(HttpStatus.BAD_REQUEST, 40012, "유효하지 않은 콘텐츠 URL입니다."),
    UNSUPPORTED_DOMAIN_TYPE(HttpStatus.BAD_REQUEST, 40013, "지원하지 않는 도메인 타입입니다."),
    NEWSLETTER_ALREADY_EXISTS(HttpStatus.CONFLICT, 40904, "이미 저장된 뉴스레터(URL)입니다."),
    CRAWLING_FAILED(HttpStatus.BAD_GATEWAY, 50201, "콘텐츠를 가져올 수 없습니다."),
    INVALID_PYTHON_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, 50001,"Python 서버 응답이 올바른 형식이 아닙니다."),

    // [Explore & Inbox]
    USER_NEWSLETTER_NOT_FOUND(HttpStatus.NOT_FOUND, 40410, "인박스 아이템을 찾을 수 없습니다."),
    USER_NEWSLETTER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, 40310, "해당 인박스 아이템에 대한 수정 권한이 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, 40411, "존재하지 않는 카테고리입니다."),
    TOPIC_NOT_FOUND(HttpStatus.NOT_FOUND, 40412, "존재하지 않는 토픽입니다."),

    // Topic and Category
    INVALID_TOPIC_CATEGORY_MATCH(HttpStatus.BAD_REQUEST, 40010, "선택한 토픽이 해당 카테고리에 속하지 않습니다."),

    // Collection
    COLLECTION_NOT_FOUND(HttpStatus.NOT_FOUND, 40401, "컬렉션을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
