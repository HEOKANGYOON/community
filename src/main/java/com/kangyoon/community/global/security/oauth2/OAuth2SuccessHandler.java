package com.kangyoon.community.global.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangyoon.community.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${oauth2.redirect-uri}")
    // "http://localhost:8080/oauth/callback.html" 로컬용
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String accessToken = jwtProvider.generateToken(oAuth2User.getEmail(), oAuth2User.getMemberId(), oAuth2User.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(oAuth2User.getEmail());

        redisTemplate.opsForValue().set(
                "refresh:" + oAuth2User.getMemberId(),
                refreshToken,
                jwtProvider.getRefreshExpiration(),
                TimeUnit.MILLISECONDS
        );

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                //TODO 배포파일 확인해봐바 secure설정 true 아닌거 같은데
                .secure(false)  //배포시 true해야함 로컬은 http라서 쿠키 전달안됨
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();
        //헤더로 쿠키(리프레시 토큰) 내려줌
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        //
        String targetUrl = UriComponentsBuilder
                .fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);



//        Map<String, Object> body = new LinkedHashMap<>();
//        body.put("message", "로그인 성공");
//        body.put("data", Map.of("accessToken", accessToken));
//
//        response.setStatus(HttpServletResponse.SC_OK);
//        response.setContentType("application/json");
//        response.setCharacterEncoding("UTF-8");
//        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
