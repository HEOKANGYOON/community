package com.kangyoon.community.global.security;

import com.kangyoon.community.global.security.oauth2.CustomOAuth2UserService;
import com.kangyoon.community.global.security.oauth2.OAuth2SuccessHandler;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public JwtProvider jwtProvider() {
        return Mockito.mock(JwtProvider.class);
    }

    @Bean
    public CustomUserDetailsService customUserDetailsService() {
        return Mockito.mock(CustomUserDetailsService.class);
    }

    @Bean
    public CustomOAuth2UserService customOAuth2UserService() {
        return Mockito.mock(CustomOAuth2UserService.class);
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return Mockito.mock(OAuth2SuccessHandler.class);
    }
}