package com.kangyoon.community.domain.member.controller;

import com.kangyoon.community.domain.member.dto.LoginRequest;
import com.kangyoon.community.domain.member.dto.LoginResponse;
import com.kangyoon.community.domain.member.dto.LoginResult;
import com.kangyoon.community.domain.member.dto.SignupRequest;
import com.kangyoon.community.domain.member.service.MemberService;
import com.kangyoon.community.global.common.ApiResponse;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Tag(name = "Member", description = "Member API")
    @Operation(
            summary = "회원가입",
            description = "입력한 email과 password로 회원가입합니다."
    )
    @PostMapping("/api/auth/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest request) {
        memberService.signup(request.email(), request.password(), request.nickname());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("회원가입 성공", null));
    }

    @Tag(name = "Member", description = "Member API")
    @Operation(
            summary = "로그인",
            description = "입력한 email과 password로 로그인합니다."
    )
    @PostMapping("/api/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        LoginResult tokens = memberService.login(request.email(), request.password());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new ApiResponse<>("로그인 성공", new LoginResponse(tokens.accessToken())));
    }

    @Tag(name = "Member", description = "Member API")
    @Operation(
            summary = "리프레시 토큰 재발급",
            description = "브라우저에 저장된 쿠키를 받아 리프레시 토큰 검증 후 재발급합니다."
    )
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletResponse response) {

        //쿠키에 refresh토큰이 없으면 직접 예외 처리
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        LoginResult tokens = memberService.refresh(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new ApiResponse<>("토큰 재발급 성공", new LoginResponse(tokens.accessToken())));
    }
}
