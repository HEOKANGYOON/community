package com.kangyoon.community.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.domain.member.dto.LoginRequest;
import com.kangyoon.community.domain.member.dto.LoginResult;
import com.kangyoon.community.domain.member.dto.SignupRequest;
import com.kangyoon.community.domain.member.service.MemberService;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.global.exception.GlobalExceptionHandler;
import com.kangyoon.community.global.security.CustomUserDetailsService;
import com.kangyoon.community.global.security.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.http.MediaType;

@WebMvcTest(MemberController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
public class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private MemberService memberService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @Test
    void 회원가입_성공() throws Exception{
        //given
        SignupRequest request = new SignupRequest("testUser@test.com", "test1234", "테스트계정");

        //when
        mockMvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    void 회원가입_중복_이메일이면_실패() throws Exception{
        //given
        SignupRequest request = new SignupRequest("testUser@test.com", "test1234", "테스트계정");
        willThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL)).given(memberService).signup(any(), any(), any());

        //when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_EMAIL.getMessage()));

    }

    @Test
    void 회원가입_중복_닉네임이면_실패() throws Exception{
        //given
        SignupRequest request = new SignupRequest("testUser@test.com", "test1234", "테스트계정");
        willThrow(new CustomException(ErrorCode.DUPLICATE_NICKNAME)).given(memberService).signup(any(), any(), any());

        //when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_NICKNAME.getMessage()));
    }

    @Test
    void 로그인_성공() throws Exception{
        //given
        LoginRequest loginRequest = new LoginRequest("testUser@test.com", "test1234");
        LoginResult loginResult = new LoginResult("accessToken", "refreshToken");

        given(memberService.login(any(), any())).willReturn(loginResult);

        //when & then
        mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그인 성공"))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().secure("refreshToken", true));
    }

    @Test
    void 로그인_실패() throws Exception {
        //given
        LoginRequest loginRequest = new LoginRequest("testUser@test.com", "test1234");

        given(memberService.login(any(), any())).willThrow(new CustomException(ErrorCode.INVALID_LOGIN));

        //when & then
        mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_LOGIN.getMessage()));
    }

    @Test
    void 리프레시_토큰_재발급() throws Exception {
        //given
        LoginResult loginResult = new LoginResult("newAccessToken", "newRefreshToken");
        given(memberService.refresh(any())).willReturn(loginResult);


        //when & then
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "refreshToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("토큰 재발급 성공"))
                .andExpect(jsonPath("$.data.accessToken").value(loginResult.accessToken()))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().value("refreshToken", "newRefreshToken"))
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }
}
