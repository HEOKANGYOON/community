package com.kangyoon.community.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /*공통*/
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다."),

    /*Member*/
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M002", "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "M003", "이미 사용 중인 닉네임입니다."),
    DUPLICATE_MEMBER(HttpStatus.CONFLICT, "M004", "이미 사용중인 이메일 또는 닉네입입니다."),
    INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "M005", "이메일, 비밀번호를 확인해주세요."),
    INVALID_OAUTH_USER(HttpStatus.UNAUTHORIZED, "M006", "유효하지 않은 OAuth 사용자입니다."),


    /*Auth*/
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다."),
    NOT_AUTHENTICATED(HttpStatus.UNAUTHORIZED, "A003", "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN,"A004", "접근 권한이 없습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED,"A005", "RefreshToken이 존재하지 않습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "A006", "RefreshToken이 일치하지 않습니다."),

    /*Board*/
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "존재하지 않는 게시판입니다."),
    BOARD_NOT_MANAGER(HttpStatus.FORBIDDEN, "B002", "관리자만 접근가능합니다."),
    BOARD_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "B003", "카테고리를 찾을 수 없습니다."),
    DUPLICATE_BOARD_NAME(HttpStatus.CONFLICT, "B004", "이미 존재하는 게시판 이름입니다."),


    /*Post*/
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "존재하지 않는 게시글입니다."),
    POST_AUTHOR_MISMATCH(HttpStatus.FORBIDDEN, "P002", "게시글 작성자가 아닙니다."),
    POST_ALREADY_DELETED(HttpStatus.FORBIDDEN, "P003", "이미 삭제된 게시글입니다."),
    DUPLICATE_VOTE(HttpStatus.FORBIDDEN, "P004", "이미 추천/비추천한 게시글입니다."),


    /*Comment*/
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "존재하지 않는 댓글입니다."),
    COMMENT_AUTHOR_MISMATCH(HttpStatus.FORBIDDEN, "CM002", "댓글 작성자가 아닙니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.FORBIDDEN, "CM003", "원댓글에만 대댓글을 허용합니다."),

    /*Notification*/
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "존재하지 않는 알림입니다."),

    /*AWS S3 IMAGE*/
    FORMAT_NOT_SUPPORTED(HttpStatus.CONFLICT, "I001", "지원하지 않는 형식입니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;
}
