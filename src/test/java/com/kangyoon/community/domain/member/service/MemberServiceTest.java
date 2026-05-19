package com.kangyoon.community.domain.member.service;

import com.kangyoon.community.domain.member.dto.LoginResult;
import com.kangyoon.community.domain.member.entity.Member;
import com.kangyoon.community.domain.member.repository.MemberRepository;
import com.kangyoon.community.global.exception.CustomException;
import com.kangyoon.community.global.exception.ErrorCode;
import com.kangyoon.community.global.security.JwtProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @Mock private MemberRepository memberRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private MemberService memberService;

    @Mock JwtProvider jwtProvider;

    @Mock RedisTemplate<String, String> redisTemplate;

    @Test
    void 회원가입_성공() {

        //given
        given(memberRepository.existsByEmail("testAccount@test.com")).willReturn(false);
        given(memberRepository.existsByNickname("testAccount")).willReturn(false);

        given(passwordEncoder.encode("test1234")).willReturn("encoded-test1234");

        //when
        memberService.signup("testAccount@test.com", "test1234", "testAccount");

        //then
        then(memberRepository).should().save(any());
    }


    @Test
    void 이메일이_중복되면_회원가입_실패() {
        //given
        given(memberRepository.existsByEmail("testAccount@test.com")).willReturn(true);

        //when & then
        assertThatThrownBy(() -> memberService.signup("testAccount@test.com", "test1234", "testAccount"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 닉네임이_중복되면_회원가입_실패() {
        //given
        given(memberRepository.existsByEmail("testAccount@test.com")).willReturn(false);
        given(memberRepository.existsByNickname("testAccount")).willReturn(true);

        //when & then
        assertThatThrownBy(() -> memberService.signup("testAccount@test.com", "test1234", "testAccount"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);
    }


    @Test
    void 로그인_성공() {
        //given
        Member mockMember = Member.createLocal("testAccount@test.com", "encoded-test1234", "testAccount");
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);


        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches("test1234", mockMember.getPassword())).willReturn(true);
        given(jwtProvider.generateToken(any(), any(), any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refreshToken");
        given(jwtProvider.getRefreshExpiration()).willReturn(86400000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        //when
        LoginResult result = memberService.login("testAccount@test.com", "test1234");

        //then
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    void 이메일이_정확하지않으면_로그인_실패() {
        //given
        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.empty());

        //when & then
        assertThatThrownBy(() -> memberService.login("testAccount@test.com", "password"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LOGIN);
    }

    @Test
    void 비밀번호가_정확하지않으면_로그인_실패() {
        //given
        Member mockMember = Member.createLocal("testAccount@test.com", "encoded-test1234", "testAccount");

        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.of(mockMember));
        given(passwordEncoder.matches("incorrectPassword", mockMember.getPassword())).willReturn(false);

        //when & then
        assertThatThrownBy(() -> memberService.login("testAccount@test.com", "incorrectPassword"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_LOGIN);
    }

    @Test
    void 리프레시_토큰_재발급_성공() {
        //given
        Member mockMember = Member.createLocal("testAccount@test.com", "encoded-test1234", "testAccount");
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        Claims mockClaims = mock(Claims.class);

        given(mockClaims.getSubject()).willReturn("testAccount@test.com");
        given(jwtProvider.parseToken("refreshToken")).willReturn(mockClaims);
        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.of(mockMember));
        given(jwtProvider.getRefreshExpiration()).willReturn(86400000L);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForValue().get("refresh:" + mockMember.getId())).willReturn("refreshToken");

        given(jwtProvider.generateToken(any(), any(), any())).willReturn("accessToken");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refreshToken");

        //when
        LoginResult result = memberService.refresh("refreshToken");

        //then
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    void 리프레시_토큰이_redis에_없으면_실패() {
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        Claims mockClaims = mock(Claims.class);
        Member mockMember = Member.createLocal("testAccount@test.com", "encoded-test1234", "testAccount");


        given(mockClaims.getSubject()).willReturn("testAccount@test.com");
        given(jwtProvider.parseToken("refreshToken")).willReturn(mockClaims);
        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.of(mockMember));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForValue().get("refresh:" + mockMember.getId())).willReturn(null);

        //when & then
        assertThatThrownBy(() -> memberService.refresh("refreshToken"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void 리프레시_토큰이_블일치하면_실패() {
        //given
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        Claims mockClaims = mock(Claims.class);
        Member mockMember = Member.createLocal("testAccount@test.com", "encoded-test1234", "testAccount");


        given(mockClaims.getSubject()).willReturn("testAccount@test.com");
        given(jwtProvider.parseToken("incorrectRefreshToken")).willReturn(mockClaims);
        given(memberRepository.findByEmail("testAccount@test.com")).willReturn(Optional.of(mockMember));
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForValue().get("refresh:" + mockMember.getId())).willReturn("refreshToken");

        //when & then
        assertThatThrownBy(() -> memberService.refresh("incorrectRefreshToken"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.REFRESH_TOKEN_MISMATCH);
    }

}
